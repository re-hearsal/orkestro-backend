package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramEventNotificationService implements EventNotificationSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @Override
    public boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text) {
        Long telegramUserId = user.getTelegramUserId();
        if (telegramUserId == null) {
            return false;
        }

        try {
            String locale = user.getPreferredLanguage() == UserLanguageType.EN ? "en" : "ru";
            Map<String, Object> payload = Map.of(
                    "telegram_user_id",
                    telegramUserId,
                    "text",
                    text,
                    "locale",
                    locale,
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

    public boolean sendEventCommentNotification(Event event, String organizationName, User user, String text) {
        Long telegramUserId = user.getTelegramUserId();
        if (telegramUserId == null) {
            return false;
        }

        try {
            String locale = user.getPreferredLanguage() == UserLanguageType.EN ? "en" : "ru";
            Map<String, Object> payload = Map.of(
                    "telegram_user_id", telegramUserId,
                    "text", text,
                    "locale", locale);

            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Telegram event comment message for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error(
                    "Failed to send Telegram event comment message to queue {} for user {}",
                    telegramBotMessageQueueName,
                    user.getId(),
                    e);
            return false;
        }
    }
}
