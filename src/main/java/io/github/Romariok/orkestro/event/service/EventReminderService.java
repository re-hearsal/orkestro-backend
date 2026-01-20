package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Периодически проверяет предстоящие события и отправляет напоминания
 * участникам за заданное количество минут до начала.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventNotificationService eventNotificationService;

    /**
     * Периодическая проверка событий, для которых пора отправить напоминание.
     * Запускается каждые 60 секунд (по умолчанию), значение можно переопределить
     * через свойство application: orkestro.events.reminder-check-interval-ms.
     */
    @Scheduled(fixedDelayString = "${orkestro.events.reminder-check-interval-ms:60000}")
    @Transactional
    public void processDueEventReminders() {
        Instant now = Instant.now();
        List<Event> eventsWithReminders = eventRepository.findByRemindBeforeMinutesIsNotNull();
        if (eventsWithReminders.isEmpty()) {
            return;
        }

        for (Event event : eventsWithReminders) {
            Integer minutesBefore = event.getRemindBeforeMinutes();
            if (minutesBefore == null) {
                continue;
            }
            Instant startTime = event.getStartTime();
            if (startTime == null) {
                continue;
            }

            Instant reminderTime = startTime.minus(minutesBefore.longValue(), ChronoUnit.MINUTES);

            if (now.isBefore(reminderTime) || !startTime.isAfter(now)) {
                continue;
            }

            List<EventParticipant> participants = eventParticipantRepository.findByEventId(event.getId());
            List<Long> userIds = participants.stream()
                    .filter(p -> p.getRsvpStatus() != EventRsvpStatus.DECLINED)
                    .map(EventParticipant::getUserId)
                    .collect(Collectors.toList());

            if (userIds.isEmpty()) {
                event.setRemindBeforeMinutes(null);
                continue;
            }

            try {
                eventNotificationService.sendEventReminderNotifications(event, userIds);
                event.setRemindBeforeMinutes(null);
            } catch (Exception ex) {
                log.error("Failed to send reminder for event {}", event.getId(), ex);
            }
        }

        eventRepository.saveAll(eventsWithReminders);
    }
}
