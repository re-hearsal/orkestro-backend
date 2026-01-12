package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRsvpListener {

    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public record EventRsvpMessage(
            @JsonProperty("telegram_user_id") Long telegramUserId,
            @JsonProperty("event_id") Long eventId,
            @JsonProperty("decision") String decision) {
    }

    @RabbitListener(queues = "${orkestro.telegram.event-rsvp-queue-name:telegram_event_rsvp}")
    @Transactional
    public void handleEventRsvp(@Payload EventRsvpMessage message) {
        if (message == null
                || message.telegramUserId() == null
                || message.eventId() == null
                || message.decision() == null) {
            log.warn("Received invalid EventRsvpMessage: {}", message);
            return;
        }

        Long telegramUserId = message.telegramUserId();
        Long eventId = message.eventId();
        String decision = message.decision();

        EventRsvpStatus newStatus = mapDecisionToStatus(decision);
        if (newStatus == null) {
            log.warn("Unknown RSVP decision '{}' for event_id={}, telegram_user_id={}", decision, eventId,
                    telegramUserId);
            return;
        }

        Optional<User> userOpt = userRepository.findByTelegramUserId(telegramUserId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for telegram_user_id={} when processing RSVP for event_id={}", telegramUserId,
                    eventId);
            return;
        }

        Long userId = userOpt.get().getId();
        Optional<EventParticipant> participantOpt = eventParticipantRepository.findByEventIdAndUserId(eventId, userId);
        if (participantOpt.isEmpty()) {
            log.warn(
                    "EventParticipant not found for event_id={} and user_id={} when processing RSVP",
                    eventId,
                    userId);
            return;
        }

        EventParticipant participant = participantOpt.get();
        participant.setRsvpStatus(newStatus);
        participant.setRsvpAt(Instant.now());
        eventParticipantRepository.save(participant);

        log.info(
                "Updated RSVP for event_id={} and user_id={} to {}",
                eventId,
                userId,
                newStatus);
    }

    private EventRsvpStatus mapDecisionToStatus(String decision) {
        String normalized = decision.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACCEPT", "ACCEPTED" -> EventRsvpStatus.ACCEPTED;
            case "DECLINE", "DECLINED", "REJECT" -> EventRsvpStatus.DECLINED;
            default -> null;
        };
    }
}
