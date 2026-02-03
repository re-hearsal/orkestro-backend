package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @Value("${orkestro.telegram.contract.type.event-rsvp:event.rsvp}")
    private String eventRsvpType;

    @Value("${orkestro.telegram.contract.status.ok:OK}")
    private String statusOk;

    @Value("${orkestro.telegram.contract.status.error:ERROR}")
    private String statusError;

    @Value("${orkestro.telegram.messages.rsvp.invalid-request:Не удалось сохранить ответ: данные запроса некорректны.}")
    private String msgRsvpInvalidRequest;

    @Value("${orkestro.telegram.messages.rsvp.unknown-decision:Не удалось сохранить ответ: неизвестное значение решения.}")
    private String msgRsvpUnknownDecision;

    @Value("${orkestro.telegram.messages.rsvp.user-not-found:Не удалось сохранить ответ: пользователь не найден.}")
    private String msgRsvpUserNotFound;

    @Value("${orkestro.telegram.messages.rsvp.participant-not-found:Не удалось сохранить ответ: вы не являетесь участником этого события.}")
    private String msgRsvpParticipantNotFound;

    @Value("${orkestro.telegram.messages.rsvp.success:✅ Ваш ответ по участию в событии сохранён.}")
    private String msgRsvpSuccess;

    @Value("${orkestro.telegram.messages.rsvp.server-error:Не удалось сохранить ответ из-за ошибки сервера. Попробуйте позже.}")
    private String msgRsvpServerError;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventRsvpMessage(
            @JsonProperty("request_id") String requestId,
            @JsonProperty("type") String type,
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
        String defaultType = (eventRsvpType == null || eventRsvpType.isBlank()) ? "event.rsvp" : eventRsvpType;
        String type = message.type() == null || message.type().isBlank() ? defaultType : message.type();
        Long telegramUserId = message.telegramUserId();
        Long eventId = message.eventId();
        String decision = message.decision();

        if (telegramUserId == null || eventId == null || decision == null) {
            log.warn("Received invalid EventRsvpMessage: {}", message);
            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    type,
                    (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                    msgRsvpInvalidRequest);
            return;
        }

        try {
            EventRsvpStatus newStatus = mapDecisionToStatus(decision);
            if (newStatus == null) {
                log.warn("Unknown RSVP decision '{}' for event_id={}, telegram_user_id={}", decision, eventId,
                        telegramUserId);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        type,
                        (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                        msgRsvpUnknownDecision);
                return;
            }

            Optional<User> userOpt = userRepository.findByTelegramUserId(telegramUserId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for telegram_user_id={} when processing RSVP for event_id={}", telegramUserId,
                        eventId);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        type,
                        (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                        msgRsvpUserNotFound);
                return;
            }

            Long userId = userOpt.get().getId();
            Optional<EventParticipant> participantOpt = eventParticipantRepository.findByEventIdAndUserId(eventId, userId);
            if (participantOpt.isEmpty()) {
                log.warn(
                        "EventParticipant not found for event_id={} and user_id={} when processing RSVP",
                        eventId,
                        userId);
                sendResultToTelegram(
                        telegramUserId,
                        requestId,
                        type,
                        (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                        msgRsvpParticipantNotFound);
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

            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    type,
                    (statusOk == null || statusOk.isBlank()) ? "OK" : statusOk,
                    msgRsvpSuccess);
        } catch (Exception ex) {
            log.error("Failed to handle RSVP message for telegram_user_id={} event_id={}", telegramUserId, eventId, ex);
            sendResultToTelegram(
                    telegramUserId,
                    requestId,
                    type,
                    (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                    msgRsvpServerError);
        }
    }

    private void sendResultToTelegram(
            Long telegramUserId,
            String requestId,
            String type,
            String status,
            String text) {
        if (telegramUserId == null) {
            log.warn("Cannot send Telegram RSVP result: telegramUserId is null");
            return;
        }
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("telegram_user_id", telegramUserId);
            payload.put("text", text);
            if (requestId != null) {
                payload.put("request_id", requestId);
            }
            if (type != null) {
                payload.put("type", type);
            }
            if (status != null) {
                payload.put("status", status);
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
}
