package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventNotificationService implements EventNotificationSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Value("${orkestro.email.queue-name:email_notifications}")
    private String emailQueueName;

    @Override
    public boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Email notification skipped for user {}: email is blank", user.getId());
            return false;
        }

        try {
            UserLanguageType language = user.getPreferredLanguage() == null ? UserLanguageType.RU : user.getPreferredLanguage();
            Locale locale = language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
            String eventTitle = event.getTitle() != null
                    ? event.getTitle()
                    : messageSource.getMessage("notification.event.title.untitled", null, locale);
            String subject = messageSource.getMessage("notification.event.email.subject.created",
                    new Object[] { eventTitle }, locale);
            EmailNotificationMessage message = new EmailNotificationMessage(
                    user.getId(),
                    event.getId(),
                    user.getEmail(),
                    subject,
                    text,
                    organizationName,
                    eventTitle,
                    true,
                    null, null, null);
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

    public boolean sendEventCommentNotification(Event event, String organizationName, User user, String text) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Email notification skipped for user {}: email is blank", user.getId());
            return false;
        }

        try {
            UserLanguageType language = user.getPreferredLanguage() == null ? UserLanguageType.RU : user.getPreferredLanguage();
            Locale locale = language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
            String eventTitle = event.getTitle() != null
                    ? event.getTitle()
                    : messageSource.getMessage("notification.event.title.untitled", null, locale);
            String subject = messageSource.getMessage("notification.event.email.subject.comment",
                    new Object[] { eventTitle }, locale);
            EmailNotificationMessage message = new EmailNotificationMessage(
                    user.getId(),
                    event.getId(),
                    user.getEmail(),
                    subject,
                    text,
                    organizationName,
                    eventTitle,
                    false,
                    null, null, null);
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(emailQueueName, payload);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email comment notification payload for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to publish email comment notification to queue {} for user {}", emailQueueName, user.getId(), e);
            return false;
        }
    }
}
