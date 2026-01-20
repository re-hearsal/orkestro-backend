package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.event.service.EventReminderService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceTest {

        @Mock
        private EventRepository eventRepository;

        @Mock
        private EventParticipantRepository eventParticipantRepository;

        @Mock
        private EventNotificationService eventNotificationService;

        @InjectMocks
        private EventReminderService eventReminderService;

        @Test
        void processDueEventReminders_noEvents_doesNothing() {
                when(eventRepository.findByRemindBeforeMinutesIsNotNull()).thenReturn(List.of());

                eventReminderService.processDueEventReminders();

                verify(eventParticipantRepository, never()).findByEventId(any(Long.class));
                verify(eventNotificationService, never()).sendEventReminderNotifications(any(Event.class),
                                anyCollection());
                verify(eventRepository, never()).saveAll(anyCollection());
        }

        @Test
        void processDueEventReminders_sendsReminderAndClearsFlag() {
                Long eventId = 1L;
                Long userIdAccepted = 10L;
                Long userIdDeclined = 20L;

                Instant now = Instant.now();
                // Событие начинается через 10 минут
                Instant startTime = now.plus(10, ChronoUnit.MINUTES);
                // Напоминание за 15 минут до начала (момент напоминания уже в прошлом)
                Integer remindBeforeMinutes = 15;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(1L)
                                .title("Rehearsal")
                                .startTime(startTime)
                                .endTime(startTime.plus(1, ChronoUnit.HOURS))
                                .remindBeforeMinutes(remindBeforeMinutes)
                                .createdAt(now.minus(1, ChronoUnit.DAYS))
                                .build();

                when(eventRepository.findByRemindBeforeMinutesIsNotNull()).thenReturn(List.of(event));

                EventParticipant accepted = EventParticipant.builder()
                                .eventId(eventId)
                                .userId(userIdAccepted)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .build();

                EventParticipant declined = EventParticipant.builder()
                                .eventId(eventId)
                                .userId(userIdDeclined)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .build();

                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of(accepted, declined));

                eventReminderService.processDueEventReminders();

                // Должно быть отправлено напоминание только пользователю без DECLINED
                verify(eventNotificationService)
                                .sendEventReminderNotifications(event, List.of(userIdAccepted));
                // После успешной отправки напоминания поле remindBeforeMinutes обнуляется
                assertNull(event.getRemindBeforeMinutes());
                verify(eventRepository).saveAll(List.of(event));
        }
}
