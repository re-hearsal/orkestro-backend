package io.github.Romariok.orkestro.organization.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.service.EmailNotificationMessage;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgNotificationService {

    private final WebSocketNotificationService wsNotificationService;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    @Value("${orkestro.email.queue-name:email_notifications}")
    private String emailQueueName;

    public void notifyJoinRequestReceived(Long organizationId, Long requestingUserId, String orgName) {
        List<Long> targetUserIds = findUsersWithPermission(organizationId, "ORG_JOIN_REQUEST_VIEW");
        targetUserIds = targetUserIds.stream()
                .filter(id -> !id.equals(requestingUserId))
                .collect(Collectors.toList());

        List<User> requestingUserList = userRepository.findAllById(List.of(requestingUserId));
        String requesterName = requestingUserList.isEmpty() ? "Unknown" : requestingUserList.get(0).getUsername();

        for (Long userId : targetUserIds) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) {
                    continue;
                }
                Locale locale = resolveLocale(user);
                String title = getMessage("notification.org.join-request.received.title", locale, orgName);
                String body = getMessage("notification.org.join-request.received.body", locale, requesterName, orgName);

                sendWsAndPreferredChannel(
                        user,
                        InAppNotificationType.JOIN_REQUEST_RECEIVED,
                        title,
                        body,
                        organizationId,
                        "organization");
            } catch (Exception e) {
                log.error("Failed to send join request received notification to user {}", userId, e);
            }
        }
    }

    public void notifyJoinRequestApproved(Long organizationId, Long applicantUserId, String orgName) {
        User user = userRepository.findById(applicantUserId).orElse(null);
        if (user == null) return;
        Locale locale = resolveLocale(user);
        String title = getMessage("notification.org.join-request.approved.title", locale, orgName);
        String body = getMessage("notification.org.join-request.approved.body", locale, orgName);
        sendWsAndPreferredChannel(user, InAppNotificationType.JOIN_REQUEST_APPROVED, title, body, organizationId, "organization");
    }

    public void notifyJoinRequestRejected(Long organizationId, Long applicantUserId, String orgName) {
        User user = userRepository.findById(applicantUserId).orElse(null);
        if (user == null) return;
        Locale locale = resolveLocale(user);
        String title = getMessage("notification.org.join-request.rejected.title", locale, orgName);
        String body = getMessage("notification.org.join-request.rejected.body", locale, orgName);
        sendWsAndPreferredChannel(user, InAppNotificationType.JOIN_REQUEST_REJECTED, title, body, organizationId, "organization");
    }


    public void notifyRoleAssigned(Long organizationId, Long memberId, String roleName, String orgName) {
        User user = userRepository.findById(memberId).orElse(null);
        if (user == null) return;
        Locale locale = resolveLocale(user);
        String title = getMessage("notification.org.role.assigned.title", locale, orgName);
        String body = getMessage("notification.org.role.assigned.body", locale, roleName, orgName);
        sendWsAndPreferredChannel(user, InAppNotificationType.ROLE_ASSIGNED, title, body, organizationId, "organization");
    }


    public void notifyRoleRemoved(Long organizationId, Long memberId, String roleName, String orgName) {
        User user = userRepository.findById(memberId).orElse(null);
        if (user == null) return;
        Locale locale = resolveLocale(user);
        String title = getMessage("notification.org.role.removed.title", locale, orgName);
        String body = getMessage("notification.org.role.removed.body", locale, roleName, orgName);
        sendWsAndPreferredChannel(user, InAppNotificationType.ROLE_REMOVED, title, body, organizationId, "organization");
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

    private List<Long> findUsersWithPermission(Long organizationId, String permissionCode) {
        List<Role> orgRoles = roleRepository.findByScopeAndOrganizationId(RoleScopeType.ORGANIZATION, organizationId);
        return orgRoles.stream()
                .filter(role -> {
                    List<String> codes = rolePermissionRepository
                            .findPermissionsByRoleId(role.getId())
                            .stream().map(p -> p.getCode()).toList();
                    return codes.contains(permissionCode);
                })
                .flatMap(role -> userRoleRepository.findByRoleId(role.getId()).stream()
                        .map(UserRole::getUserId))
                .distinct()
                .filter(userId -> organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                        .isPresent())
                .collect(Collectors.toList());
    }

    private Locale resolveLocale(User user) {
        if (user.getPreferredLanguage() == null) return Locale.forLanguageTag("ru");
        return user.getPreferredLanguage() == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }

    private String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }
}
