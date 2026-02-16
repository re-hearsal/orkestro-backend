package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.service.EventRsvpListener;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class EventRsvpListenerTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private EventParticipantRepository eventParticipantRepository;

        @Mock
        private RabbitTemplate rabbitTemplate;

        @Mock
        private MessageSource messageSource;

        private EventRsvpListener listener;

        private final ObjectMapper objectMapper = new ObjectMapper();

        private Message buildMessage(Long telegramUserId, Long eventId, String decision) throws Exception {
                String json = objectMapper.writeValueAsString(Map.of(
                                "request_id", "req",
                                "type", "event.rsvp",
                                "telegram_user_id", telegramUserId,
                                "event_id", eventId,
                                "decision", decision));
                MessageProperties props = new MessageProperties();
                props.setContentType("application/json");
                return new Message(json.getBytes(java.nio.charset.StandardCharsets.UTF_8), props);
        }

        private void initListener() throws Exception {
                listener = new EventRsvpListener(
                                userRepository,
                                eventParticipantRepository,
                                rabbitTemplate,
                                objectMapper,
                                messageSource);
                Field field = EventRsvpListener.class.getDeclaredField("telegramBotMessageQueueName");
                field.setAccessible(true);
                field.set(listener, "telegram_bot_messages");
        }

        @Test
        void handleEventRsvp_nullMessage_doesNothing() throws Exception {
                initListener();
                listener.handleEventRsvp(null);

                verifyNoInteractions(userRepository);
                verifyNoInteractions(eventParticipantRepository);
                verifyNoInteractions(rabbitTemplate);
        }

        @Test
        void handleEventRsvp_unknownDecision_doesNotUpdateParticipant() throws Exception {
                initListener();
                Message message = buildMessage(123L, 10L, "MAYBE");

                listener.handleEventRsvp(message);

                verifyNoInteractions(userRepository);
                verifyNoInteractions(eventParticipantRepository);
        }

        @Test
        void handleEventRsvp_userNotFound_doesNotUpdateParticipant() throws Exception {
                initListener();
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Message message = buildMessage(telegramUserId, eventId, "ACCEPT");

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());

                listener.handleEventRsvp(message);

                verify(userRepository).findByTelegramUserId(telegramUserId);
                verify(eventParticipantRepository, never()).findByEventIdAndUserId(anyLong(), anyLong());
        }

        @Test
        void handleEventRsvp_participantNotFound_doesNotSave() throws Exception {
                initListener();
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                Message message = buildMessage(telegramUserId, eventId, "ACCEPT");

                User user = User.builder().id(userId).username("u").email("e").password("p").profileImageFileId(1L)
                                .build();

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user));
                when(eventParticipantRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

                listener.handleEventRsvp(message);

                verify(eventParticipantRepository).findByEventIdAndUserId(eventId, userId);
                verify(eventParticipantRepository, never()).save(any(EventParticipant.class));
        }

        @Test
        void handleEventRsvp_accept_updatesRsvpStatusAndTimestamp() throws Exception {
                initListener();
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                Message message = buildMessage(telegramUserId, eventId, "ACCEPT");

                User user = User.builder().id(userId).username("u").email("e").password("p").profileImageFileId(1L)
                                .build();

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user));

                EventParticipant participant = new EventParticipant();
                participant.setEventId(eventId);
                participant.setUserId(userId);
                participant.setRsvpStatus(EventRsvpStatus.PENDING);
                participant.setRsvpAt(null);

                when(eventParticipantRepository.findByEventIdAndUserId(eventId, userId))
                                .thenReturn(Optional.of(participant));

                listener.handleEventRsvp(message);

                ArgumentCaptor<EventParticipant> captor = ArgumentCaptor.forClass(EventParticipant.class);
                verify(eventParticipantRepository).save(captor.capture());
                EventParticipant saved = captor.getValue();

                org.junit.jupiter.api.Assertions.assertEquals(EventRsvpStatus.ACCEPTED, saved.getRsvpStatus());
                org.junit.jupiter.api.Assertions.assertEquals(eventId, saved.getEventId());
                org.junit.jupiter.api.Assertions.assertEquals(userId, saved.getUserId());
                org.junit.jupiter.api.Assertions.assertNotNull(saved.getRsvpAt());
        }

        @Test
        void handleEventRsvp_decline_updatesRsvpStatus() throws Exception {
                initListener();
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                Message message = buildMessage(telegramUserId, eventId, "DECLINE");

                User user = User.builder().id(userId).username("u").email("e").password("p").profileImageFileId(1L)
                                .build();

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user));

                EventParticipant participant = new EventParticipant();
                participant.setEventId(eventId);
                participant.setUserId(userId);
                participant.setRsvpStatus(EventRsvpStatus.PENDING);
                participant.setRsvpAt(Instant.now());

                when(eventParticipantRepository.findByEventIdAndUserId(eventId, userId))
                                .thenReturn(Optional.of(participant));

                listener.handleEventRsvp(message);

                ArgumentCaptor<EventParticipant> captor = ArgumentCaptor.forClass(EventParticipant.class);
                verify(eventParticipantRepository).save(captor.capture());
                EventParticipant saved = captor.getValue();

                org.junit.jupiter.api.Assertions.assertEquals(EventRsvpStatus.DECLINED, saved.getRsvpStatus());
                org.junit.jupiter.api.Assertions.assertEquals(eventId, saved.getEventId());
                org.junit.jupiter.api.Assertions.assertEquals(userId, saved.getUserId());
        }
}
