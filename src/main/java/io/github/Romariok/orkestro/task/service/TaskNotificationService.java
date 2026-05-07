package io.github.Romariok.orkestro.task.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.service.EmailNotificationMessage;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskNotificationService {

    private final WebSocketNotificationService wsNotificationService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${orkestro.email.queue-name:email_notifications}")
    private String emailQueueName;

    /**
     * Рассылает TASK_UPDATED всем участникам задачи (автор + исполнители), кроме инициатора изменения.
     * Также публикует агрегированный WS-топик для организации.
     */
    public void notifyTaskUpdated(Long organizationId, Long taskId, String taskTitle,
            Long authorUserId, List<Long> assigneeUserIds, Long initiatorUserId) {
        String orgName = resolveOrgName(organizationId);
        List<Long> recipients = buildRecipients(authorUserId, assigneeUserIds, initiatorUserId);
        for (Long userId : recipients) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;
                Locale locale = resolveLocale(user);
                String title = getMessage("notification.task.updated.title", locale, taskTitle);
                String body = getMessage("notification.task.updated.body", locale, taskTitle, orgName);
                sendWsAndPreferredChannel(user, InAppNotificationType.TASK_UPDATED, title, body, taskId, "TASK");
            } catch (Exception e) {
                log.error("Failed to send TASK_UPDATED notification to user {}", userId, e);
            }
        }
        broadcastToOrgTopic(organizationId, taskId, "TASK_UPDATED");
    }

    /**
     * Рассылает TASK_DELETED всем участникам задачи (автор + исполнители), кроме инициатора.
     */
    public void notifyTaskDeleted(Long organizationId, Long taskId, String taskTitle,
            Long authorUserId, List<Long> assigneeUserIds, Long initiatorUserId) {
        String orgName = resolveOrgName(organizationId);
        List<Long> recipients = buildRecipients(authorUserId, assigneeUserIds, initiatorUserId);
        for (Long userId : recipients) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;
                Locale locale = resolveLocale(user);
                String title = getMessage("notification.task.deleted.title", locale, taskTitle);
                String body = getMessage("notification.task.deleted.body", locale, taskTitle, orgName);
                sendWsAndPreferredChannel(user, InAppNotificationType.TASK_DELETED, title, body, taskId, "TASK");
            } catch (Exception e) {
                log.error("Failed to send TASK_DELETED notification to user {}", userId, e);
            }
        }
        broadcastToOrgTopic(organizationId, taskId, "TASK_DELETED");
    }

    /**
     * Уведомляет пользователя о том, что его добавили в исполнители.
     */
    public void notifyAssigneeAdded(Long organizationId, Long taskId, String taskTitle, Long addedUserId) {
        try {
            User user = userRepository.findById(addedUserId).orElse(null);
            if (user == null) return;
            String orgName = resolveOrgName(organizationId);
            Locale locale = resolveLocale(user);
            String title = getMessage("notification.task.assignee.added.title", locale);
            String body = getMessage("notification.task.assignee.added.body", locale, taskTitle, orgName);
            sendWsAndPreferredChannel(user, InAppNotificationType.TASK_ASSIGNEE_ADDED, title, body, taskId, "TASK");
        } catch (Exception e) {
            log.error("Failed to send TASK_ASSIGNEE_ADDED notification to user {}", addedUserId, e);
        }
    }

    /**
     * Уведомляет пользователя о том, что его удалили из исполнителей.
     */
    public void notifyAssigneeRemoved(Long organizationId, Long taskId, String taskTitle, Long removedUserId) {
        try {
            User user = userRepository.findById(removedUserId).orElse(null);
            if (user == null) return;
            String orgName = resolveOrgName(organizationId);
            Locale locale = resolveLocale(user);
            String title = getMessage("notification.task.assignee.removed.title", locale);
            String body = getMessage("notification.task.assignee.removed.body", locale, taskTitle, orgName);
            sendWsAndPreferredChannel(user, InAppNotificationType.TASK_ASSIGNEE_REMOVED, title, body, taskId, "TASK");
        } catch (Exception e) {
            log.error("Failed to send TASK_ASSIGNEE_REMOVED notification to user {}", removedUserId, e);
        }
    }

    /**
     * Уведомляет остальных участников (автор + исполнители, кроме инициатора) об изменении состава исполнителей.
     * Пользователи из newlyAddedUserIds исключаются — они уже получили персональное уведомление о добавлении.
     */
    public void notifyAssigneesChanged(Long organizationId, Long taskId, String taskTitle,
            Long authorUserId, List<Long> assigneeUserIds, Long initiatorUserId) {
        notifyAssigneesChanged(organizationId, taskId, taskTitle, authorUserId, assigneeUserIds, initiatorUserId,
                List.of());
    }

    /**
     * Уведомляет остальных участников (автор + исполнители, кроме инициатора и newlyAddedUserIds) об изменении
     * состава исполнителей. Пользователи из newlyAddedUserIds исключаются — они уже получили персональное
     * уведомление о добавлении.
     */
    public void notifyAssigneesChanged(Long organizationId, Long taskId, String taskTitle,
            Long authorUserId, List<Long> assigneeUserIds, Long initiatorUserId, List<Long> newlyAddedUserIds) {
        String orgName = resolveOrgName(organizationId);
        List<Long> recipients = buildRecipients(authorUserId, assigneeUserIds, initiatorUserId);
        java.util.Set<Long> skipSet = newlyAddedUserIds != null ? new java.util.HashSet<>(newlyAddedUserIds)
                : java.util.Collections.emptySet();
        for (Long userId : recipients) {
            if (skipSet.contains(userId)) continue;
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;
                Locale locale = resolveLocale(user);
                String title = getMessage("notification.task.assignees.changed.title", locale, taskTitle);
                String body = getMessage("notification.task.assignees.changed.body", locale, taskTitle, orgName);
                sendWsAndPreferredChannel(user, InAppNotificationType.TASK_UPDATED, title, body, taskId, "TASK");
            } catch (Exception e) {
                log.error("Failed to send assignees-changed notification to user {}", userId, e);
            }
        }
        broadcastToOrgTopic(organizationId, taskId, "TASK_UPDATED");
    }

    /**
     * Уведомляет автора и исполнителей о просроченном дедлайне.
     */
    public void notifyDeadlineOverdue(Long organizationId, Long taskId, String taskTitle,
            Long authorUserId, List<Long> assigneeUserIds) {
        String orgName = resolveOrgName(organizationId);
        List<Long> recipients = buildRecipients(authorUserId, assigneeUserIds, null);
        for (Long userId : recipients) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) continue;
                Locale locale = resolveLocale(user);
                String title = getMessage("notification.task.deadline.overdue.title", locale, taskTitle);
                String body = getMessage("notification.task.deadline.overdue.body", locale, taskTitle, orgName);
                sendWsAndPreferredChannel(user, InAppNotificationType.TASK_DEADLINE_OVERDUE, title, body, taskId, "TASK");
            } catch (Exception e) {
                log.error("Failed to send TASK_DEADLINE_OVERDUE notification to user {}", userId, e);
            }
        }
    }

    private String resolveOrgName(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .map(org -> org.getName())
                .orElse(String.valueOf(organizationId));
    }

    private void broadcastToOrgTopic(Long organizationId, Long taskId, String type) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", type);
            payload.put("taskId", taskId);
            messagingTemplate.convertAndSend(
                    "/topic/organizations/" + organizationId + "/tasks",
                    (Object) payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast {} to org topic for task {}", type, taskId, e);
        }
    }

    private List<Long> buildRecipients(Long authorUserId, List<Long> assigneeUserIds, Long excludeUserId) {
        java.util.Set<Long> set = new java.util.LinkedHashSet<>();
        if (authorUserId != null) set.add(authorUserId);
        if (assigneeUserIds != null) set.addAll(assigneeUserIds);
        if (excludeUserId != null) set.remove(excludeUserId);
        return List.copyOf(set);
    }

    private void sendWsAndPreferredChannel(User user, InAppNotificationType type,
            String title, String body, Long entityId, String entityType) {
        try {
            wsNotificationService.send(user.getId(), InAppNotificationDTO.builder()
                    .type(type)
                    .title(title)
                    .body(body)
                    .entityId(entityId)
                    .entityType(entityType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to send WS notification to user {}", user.getId(), e);
        }

        try {
            NotificationChannelType channel = user.getNotificationChannel();
            if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
                sendTelegramNotification(user, body);
            } else if (channel == NotificationChannelType.VK && user.getVkUserId() != null) {
                sendVkNotification(user, body);
            } else {
                sendEmailNotification(user, title, body);
            }
        } catch (Exception e) {
            log.error("Failed to send preferred-channel notification to user {}", user.getId(), e);
        }
    }

    private void sendEmailNotification(User user, String subject, String text) {
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        try {
            EmailNotificationMessage message = new EmailNotificationMessage(
                    user.getId(), null, user.getEmail(), subject, text, null, null, false, null, null, null);
            String payload = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(emailQueueName, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize email notification for user {}", user.getId(), e);
        }
    }

    private void sendTelegramNotification(User user, String text) {
        try {
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "telegram_user_id", user.getTelegramUserId(),
                    "text", text,
                    "locale", user.getPreferredLanguage() == UserLanguageType.EN ? "en" : "ru",
                    "buttons", List.of());
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend("telegram_bot_messages", json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to send Telegram notification to user {}, falling back to email", user.getId());
            sendEmailNotification(user, text, text);
        }
    }

    private void sendVkNotification(User user, String text) {
        try {
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "vk_user_id", user.getVkUserId(),
                    "text", text);
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend("vk_bot_messages", json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to send VK notification to user {}, falling back to email", user.getId());
            sendEmailNotification(user, text, text);
        }
    }

    private Locale resolveLocale(User user) {
        if (user.getPreferredLanguage() == null) return Locale.forLanguageTag("ru");
        return user.getPreferredLanguage() == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }

    private String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
