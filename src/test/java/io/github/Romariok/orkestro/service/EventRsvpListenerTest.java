package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.service.EventRsvpListener;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRsvpListenerTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private EventParticipantRepository eventParticipantRepository;

        @InjectMocks
        private EventRsvpListener listener;

        @Test
        void handleEventRsvp_nullMessage_doesNothing() {
                listener.handleEventRsvp(null);

                verifyNoInteractions(userRepository);
                verifyNoInteractions(eventParticipantRepository);
        }

        @Test
        void handleEventRsvp_unknownDecision_doesNotUpdateParticipant() {
                EventRsvpListener.EventRsvpMessage message = new EventRsvpListener.EventRsvpMessage(123L, 10L, "MAYBE");

                listener.handleEventRsvp(message);

                verifyNoInteractions(userRepository);
                verifyNoInteractions(eventParticipantRepository);
        }

        @Test
        void handleEventRsvp_userNotFound_doesNotUpdateParticipant() {
                Long telegramUserId = 123L;
                Long eventId = 10L;
                EventRsvpListener.EventRsvpMessage message = new EventRsvpListener.EventRsvpMessage(telegramUserId,
                                eventId, "ACCEPT");

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());

                listener.handleEventRsvp(message);

                verify(userRepository).findByTelegramUserId(telegramUserId);
                verify(eventParticipantRepository, never()).findByEventIdAndUserId(anyLong(), anyLong());
        }

        @Test
        void handleEventRsvp_participantNotFound_doesNotSave() {
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                EventRsvpListener.EventRsvpMessage message = new EventRsvpListener.EventRsvpMessage(telegramUserId,
                                eventId, "ACCEPT");

                User user = User.builder().id(userId).username("u").email("e").password("p").profileImageFileId(1L)
                                .build();

                when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(user));
                when(eventParticipantRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(Optional.empty());

                listener.handleEventRsvp(message);

                verify(eventParticipantRepository).findByEventIdAndUserId(eventId, userId);
                verify(eventParticipantRepository, never()).save(any(EventParticipant.class));
        }

        @Test
        void handleEventRsvp_accept_updatesRsvpStatusAndTimestamp() {
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                EventRsvpListener.EventRsvpMessage message = new EventRsvpListener.EventRsvpMessage(telegramUserId,
                                eventId, "ACCEPT");

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
        void handleEventRsvp_decline_updatesRsvpStatus() {
                Long telegramUserId = 123L;
                Long eventId = 10L;
                Long userId = 5L;
                EventRsvpListener.EventRsvpMessage message = new EventRsvpListener.EventRsvpMessage(telegramUserId,
                                eventId, "DECLINE");

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
