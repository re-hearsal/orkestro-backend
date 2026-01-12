package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Отправка уведомлений участникам события о создании мероприятия.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @Value("${orkestro.mail.from:no-reply@orkestro.local}")
    private String fromAddress;

    public void sendEventCreatedNotifications(Event event, Collection<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>(participantUserIds);
        List<User> users = userRepository.findAllById(uniqueIds);
        if (users.isEmpty()) {
            return;
        }

        String organizationName = organizationRepository.findById(event.getOrganizationId())
                .map(Organization::getName)
                .orElse("организации");

        for (User user : users) {
            try {
                sendToUser(event, organizationName, user);
            } catch (Exception e) {
                log.error(
                        "Failed to send event created notification for event {} to user {}",
                        event.getId(),
                        user.getId(),
                        e);
            }
        }
    }

    private void sendToUser(Event event, String organizationName, User user) {
        String text = buildInviteText(event, organizationName);
        NotificationChannelType channel = user.getNotificationChannel();

        if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
            boolean telegramOk = sendTelegramInvite(event, user, text);
            if (!telegramOk) {
                sendEmailInvite(event, organizationName, user, text);
            }
        } else {
            sendEmailInvite(event, organizationName, user, text);
        }
    }

    private String buildInviteText(Event event, String organizationName) {
        String title = event.getTitle() != null ? event.getTitle() : "без названия";
        return "Вас пригласили на мероприятие \"" + title + "\" в организации \"" + organizationName
                + "\". Просим вас сообщить о вашем присутствии.";
    }

    private boolean sendTelegramInvite(Event event, User user, String text) {
        Long telegramUserId = user.getTelegramUserId();
        if (telegramUserId == null) {
            return false;
        }

        try {
            Map<String, Object> payload = Map.of(
                    "telegram_user_id",
                    telegramUserId,
                    "text",
                    text,
                    "buttons",
                    List.of(
                            Map.of(
                                    "type", "event_rsvp",
                                    "event_id", event.getId(),
                                    "action", "ACCEPT"),
                            Map.of(
                                    "type", "event_rsvp",
                                    "event_id", event.getId(),
                                    "action", "DECLINE")));

            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Telegram event invite message for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error(
                    "Failed to send Telegram event invite message to queue {} for user {}",
                    telegramBotMessageQueueName,
                    user.getId(),
                    e);
            return false;
        }
    }

    private void sendEmailInvite(Event event, String organizationName, User user, String text) {
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
        } catch (MessagingException e) {
            log.error("Failed to construct email event invite message for user {}", user.getId(), e);
        } catch (Exception e) {
            log.error("Failed to send email event invite message via SMTP for user {}", user.getId(), e);
        }
    }
}
