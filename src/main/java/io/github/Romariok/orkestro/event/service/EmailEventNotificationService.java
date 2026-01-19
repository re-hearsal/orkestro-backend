package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Sends event notifications to users via email (SMTP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailEventNotificationService implements EventNotificationSender {

    private final JavaMailSender mailSender;

    @Value("${orkestro.mail.from:no-reply@orkestro.local}")
    private String fromAddress;

    @Override
    public boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text) {
        String subject = "Приглашение на мероприятие \"" + event.getTitle() + "\"";

        String htmlBody = "<p>" + text + "</p>"
                + "<p>"
                + "<b>Организация:</b> " + organizationName + "<br/>"
                + "<b>Мероприятие:</b> " + (event.getTitle() != null ? event.getTitle() : "без названия")
                + "</p>"
                + "<p>"
                + "<button>Я приду</button>&nbsp;&nbsp;"
                + "<button>Не смогу прийти</button>"
                + "</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to construct email event invite message for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to send email event invite message via SMTP for user {}", user.getId(), e);
            return false;
        }
    }
}
