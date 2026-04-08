package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventRsvpListener {

    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventRsvpMessage(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("telegram_user_id") Long telegramUserId,
            @JsonProperty("event_id") Long eventId,
            @JsonProperty("decision") String decision) {
    }

    @RabbitListener(queues = "${orkestro.telegram.event-rsvp-queue-name:telegram_event_rsvp}")
    @Transactional
    public void handleEventRsvp(Message amqpMessage) {
        if (amqpMessage == null || amqpMessage.getBody() == null) {
            log.warn("Received null/empty AMQP message for RSVP");
            return;
        }

        EventRsvpMessage message;
        try {
            message = objectMapper.readValue(amqpMessage.getBody(), EventRsvpMessage.class);
        } catch (Exception ex) {
            log.error("Failed to parse EventRsvpMessage from RabbitMQ", ex);
            return;
        }

        String requestId = message.requestId();
        Long telegramUserId = message.telegramUserId();
        Long eventId = message.eventId();
        String decision = message.decision();

        if (telegramUserId == null || eventId == null || decision == null) {
            log.warn("Received invalid EventRsvpMessage: {}", message);
            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    Locale.forLanguageTag("ru"),
                    getMessage("notification.telegram.rsvp.invalid-request", Locale.forLanguageTag("ru")));
            return;
        }

        Optional<User> userOpt = Optional.empty();
        try {
            EventRsvpStatus newStatus = mapDecisionToStatus(decision);
            if (newStatus == null) {
                log.warn("Unknown RSVP decision '{}' for event_id={}, telegram_user_id={}", decision, eventId,
                        telegramUserId);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        Locale.forLanguageTag("ru"),
                        getMessage("notification.telegram.rsvp.unknown-decision", Locale.forLanguageTag("ru")));
                return;
            }

            userOpt = userRepository.findByTelegramUserId(telegramUserId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for telegram_user_id={} when processing RSVP for event_id={}", telegramUserId,
                        eventId);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        Locale.forLanguageTag("ru"),
                        getMessage("notification.telegram.rsvp.user-not-found", Locale.forLanguageTag("ru")));
                return;
            }

            User user = userOpt.get();
            Long userId = user.getId();
            Optional<EventParticipant> participantOpt = eventParticipantRepository.findByEventIdAndUserId(eventId, userId);
            if (participantOpt.isEmpty()) {
                log.warn(
                        "EventParticipant not found for event_id={} and user_id={} when processing RSVP",
                        eventId,
                        userId);
                Locale locale = resolveLocale(user);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        locale,
                        getMessage("notification.telegram.rsvp.participant-not-found", locale));
                return;
            }

            EventParticipant participant = participantOpt.get();
            participant.setRsvpStatus(newStatus);
            participant.setRsvpAt(Instant.now());
            eventParticipantRepository.save(participant);

            log.info("Updated RSVP for event_id={} and user_id={} to {}", eventId, userId, newStatus);

            Locale locale = resolveLocale(user);
            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    locale,
                    getMessage("notification.telegram.rsvp.success", locale));
        } catch (Exception ex) {
            log.error("Failed to handle RSVP message for telegram_user_id={} event_id={}", telegramUserId, eventId, ex);
            Locale locale = userOpt.map(this::resolveLocale).orElse(Locale.forLanguageTag("ru"));
            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    locale,
                    getMessage("notification.telegram.rsvp.server-error", locale));
        }
    }

    private void sendResultToTelegram(
            Long telegramUserId,
            String requestId,
            Locale locale,
            String text) {
        if (telegramUserId == null) {
            log.warn("Cannot send Telegram RSVP result: telegramUserId is null");
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("telegram_user_id", telegramUserId);
            payload.put("text", text);
            payload.put("locale", locale.getLanguage().equals("en") ? "en" : "ru");
            if (requestId != null) {
                payload.put("request_id", requestId);
            }
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Telegram RSVP result message", e);
        } catch (Exception e) {
            log.error("Failed to send Telegram RSVP result message to queue {}", telegramBotMessageQueueName, e);
        }
    }

    private EventRsvpStatus mapDecisionToStatus(String decision) {
        String normalized = decision.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ACCEPT", "ACCEPTED" -> EventRsvpStatus.ACCEPTED;
            case "DECLINE", "DECLINED", "REJECT" -> EventRsvpStatus.DECLINED;
            default -> null;
        };
    }

    private Locale resolveLocale(User user) {
        UserLanguageType language = user.getPreferredLanguage() == null ? UserLanguageType.RU : user.getPreferredLanguage();
        return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }

    private String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}
