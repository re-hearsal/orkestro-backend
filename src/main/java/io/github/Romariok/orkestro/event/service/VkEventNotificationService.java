package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.service.notification.NotificationMessage;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.User;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class VkEventNotificationService implements EventNotificationSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${orkestro.vk.bot-message-queue-name:vk_notification_queue}")
    private String vkBotMessageQueueName;

    @Override
    public boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text) {
        Long vkUserId = user.getVkUserId();
        if (vkUserId == null) {
            return false;
        }

        try {
            NotificationMessage message = new NotificationMessage(
                    user.getId(),
                    NotificationChannelType.VK,
                    "event.created",
                    text,
                    Map.of(
                            "vk_user_id", vkUserId,
                            "event_id", event.getId()));

            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize VK event invite message for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error(
                    "Failed to send VK event invite message to queue {} for user {}",
                    vkBotMessageQueueName,
                    user.getId(),
                    e);
            return false;
        }
    }

    public boolean sendEventCommentNotification(Event event, String organizationName, User user, String text) {
        Long vkUserId = user.getVkUserId();
        if (vkUserId == null) {
            return false;
        }

        try {
            NotificationMessage message = new NotificationMessage(
                    user.getId(),
                    NotificationChannelType.VK,
                    "event.comment",
                    text,
                    Map.of("vk_user_id", vkUserId));

            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize VK event comment message for user {}", user.getId(), e);
            return false;
        } catch (Exception e) {
            log.error(
                    "Failed to send VK event comment message to queue {} for user {}",
                    vkBotMessageQueueName,
                    user.getId(),
                    e);
            return false;
        }
    }
}
