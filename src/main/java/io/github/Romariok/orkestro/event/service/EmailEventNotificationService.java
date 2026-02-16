package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Produces event notifications to RabbitMQ for asynchronous email sending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventNotificationService implements EventNotificationSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${orkestro.email.queue-name:email_notifications}")
    private String emailQueueName;

    @Override
    public boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Email notification skipped for user {}: email is blank", user.getId());
            return false;
        }

        try {
            String eventTitle = event.getTitle() != null ? event.getTitle() : "без названия";
            String subject = "Приглашение на мероприятие \"" + eventTitle + "\"";
            EmailNotificationMessage message = new EmailNotificationMessage(
                    user.getId(),
                    event.getId(),
                    user.getEmail(),
                    subject,
                    text,
                    organizationName,
                    eventTitle);
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(emailQueueName, payload);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email notification payload for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to publish email notification to queue {} for user {}", emailQueueName, user.getId(), e);
            return false;
        }
    }
}
