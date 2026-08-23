package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import io.github.Romariok.orkestro.mail.service.JcaEmailService;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationListener {
    private final ObjectMapper objectMapper;
    private final JcaEmailService jcaEmailService;
    private final EmailRsvpTokenService emailRsvpTokenService;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    @Value("${orkestro.email.rsvp.base-url:http://localhost:8080}")
    private String rsvpBaseUrl;

    @RabbitListener(queues = "${orkestro.email.queue-name:email_notifications}")
    public void handleEmailNotification(Message amqpMessage) {
        if (amqpMessage == null || amqpMessage.getBody() == null) {
            log.warn("Received null/empty AMQP email message");
            return;
        }

        try {
            EmailNotificationMessage message = objectMapper.readValue(amqpMessage.getBody(), EmailNotificationMessage.class);
            if (message.to() == null || message.to().isBlank()) {
                log.warn("Skipping email message without recipient: userId={}", message.userId());
                return;
            }
            Locale locale = resolveLocale(message.userId());

            String templateName = message.template() != null ? message.template() : "event-invite.html";
            Map<String, Object> templateModel = new HashMap<>();
            templateModel.put("lang", locale.getLanguage());

            if ("org-notification.html".equals(templateName)) {
                templateModel.put("emailTitle", messageSource.getMessage("notification.org.info-message.email.title", null, locale));
                templateModel.put("text", message.text());
                templateModel.put("authorLabel", messageSource.getMessage("notification.org.info-message.label.author", null, locale));
                templateModel.put("authorName", message.authorName() != null ? message.authorName() : "");
                templateModel.put("organizationLabel", messageSource.getMessage("notification.org.info-message.label.organization", null, locale));
                templateModel.put("organizationName", message.organizationName() != null ? message.organizationName() : "");
                boolean hasSection = message.sectionName() != null && !message.sectionName().isBlank();
                templateModel.put("hasSection", hasSection);
                templateModel.put("sectionLabel", messageSource.getMessage("notification.org.info-message.label.section", null, locale));
                templateModel.put("sectionName", hasSection ? message.sectionName() : "");
            } else {
                templateModel.put("text", message.text());
                templateModel.put("organizationName", message.organizationName());
                templateModel.put("eventTitle", message.eventTitle());
                templateModel.put("emailTitle", messageSource.getMessage("notification.email.template.title", null, locale));
                templateModel.put("organizationLabel",
                        messageSource.getMessage("notification.email.template.label.organization", null, locale));
                templateModel.put("eventLabel",
                        messageSource.getMessage("notification.email.template.label.event", null, locale));
                boolean includeRsvpForm = message.includeRsvpForm() == null || message.includeRsvpForm();
                templateModel.put("includeRsvpForm", includeRsvpForm);
                if (includeRsvpForm) {
                    templateModel.put("attendButtonLabel",
                            messageSource.getMessage("notification.email.template.button.attend", null, locale));
                    templateModel.put("absentButtonLabel",
                            messageSource.getMessage("notification.email.template.button.absent", null, locale));
                    templateModel.put("attendUrl", buildRsvpUrl(message.eventId(), message.userId(), true));
                    templateModel.put("absentUrl", buildRsvpUrl(message.eventId(), message.userId(), false));
                }
                boolean hasEventLink = message.eventLink() != null && !message.eventLink().isBlank();
                templateModel.put("hasEventLink", hasEventLink);
                if (hasEventLink) {
                    templateModel.put("eventLink", message.eventLink());
                    templateModel.put("viewEventButtonLabel",
                            messageSource.getMessage("notification.email.template.button.view-event", null, locale));
                }
            }

            jcaEmailService.sendTemplateMessage(message.to(), message.subject(), templateName, templateModel);
        } catch (IOException | RuntimeException ex) {
            log.error("Failed to process RabbitMQ email notification message", ex);
        }
    }

    private String buildRsvpUrl(Long eventId, Long userId, boolean accepted) {
        String token = emailRsvpTokenService.createToken(eventId, userId, accepted);
        return rsvpBaseUrl + "/api/v1/events/rsvp/email?token=" + UriUtils.encode(token, java.nio.charset.StandardCharsets.UTF_8);
    }

    private Locale resolveLocale(Long userId) {
        if (userId == null) {
            return Locale.forLanguageTag("ru");
        }
        UserLanguageType language = userRepository.findById(userId)
                .map(user -> user.getPreferredLanguage())
                .orElse(UserLanguageType.RU);
        return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }
}
