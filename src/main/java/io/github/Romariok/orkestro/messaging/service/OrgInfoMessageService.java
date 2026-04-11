package io.github.Romariok.orkestro.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.service.notification.NotificationMessage;
import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageDTO;
import io.github.Romariok.orkestro.messaging.models.OrgInfoMessage;
import io.github.Romariok.orkestro.messaging.repository.OrgInfoMessageRepository;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgInfoMessageService {

    private final OrgInfoMessageRepository orgInfoMessageRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final SectionUserRepository sectionUserRepository;
    private final SectionRepository sectionRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocketNotificationService webSocketNotificationService;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @Value("${orkestro.vk.bot-message-queue-name:vk_notification_queue}")
    private String vkBotMessageQueueName;

    @Value("${orkestro.email.queue-name:email_notifications}")
    private String emailQueueName;

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_WRITE_INFO')")
    public OrgInfoMessageDTO postOrgMessage(Long organizationId, String text) {
        Long authorId = securityUtils.getCurrentUserId();

        OrgInfoMessage message = OrgInfoMessage.builder()
                .organizationId(organizationId)
                .sectionId(null)
                .authorUserId(authorId)
                .text(text)
                .build();
        OrgInfoMessage saved = orgInfoMessageRepository.save(message);

        User author = userRepository.findById(authorId).orElse(null);
        OrgInfoMessageDTO dto = toDTO(saved, author);

        List<OrganizationUser> members = organizationUserRepository
                .findByOrganizationIdAndStatus(organizationId, OrganizationUserStatusType.ACCEPTED);
        List<Long> memberUserIds = members.stream()
                .map(OrganizationUser::getUserId)
                .filter(id -> !id.equals(authorId))
                .distinct()
                .toList();
        sendNotificationsToUsers(memberUserIds, text, saved.getId());

        return dto;
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_WRITE_INFO')")
    public OrgInfoMessageDTO postSectionMessage(Long sectionId, String text) {
        Long authorId = securityUtils.getCurrentUserId();

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found: " + sectionId));

        OrgInfoMessage message = OrgInfoMessage.builder()
                .organizationId(section.getOrganizationId())
                .sectionId(sectionId)
                .authorUserId(authorId)
                .text(text)
                .build();
        OrgInfoMessage saved = orgInfoMessageRepository.save(message);

        User author = userRepository.findById(authorId).orElse(null);
        OrgInfoMessageDTO dto = toDTO(saved, author);

        List<SectionUser> sectionUsers = sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId);
        List<Long> memberUserIds = sectionUsers.stream()
                .map(SectionUser::getUserId)
                .filter(id -> !id.equals(authorId))
                .distinct()
                .toList();
        sendNotificationsToUsers(memberUserIds, text, saved.getId());

        return dto;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
    public Page<OrgInfoMessageDTO> getOrgMessages(Long organizationId, Pageable pageable) {
        Page<OrgInfoMessage> page = orgInfoMessageRepository
                .findByOrganizationIdAndSectionIdIsNullOrderByCreatedAtDesc(organizationId, pageable);

        List<Long> authorIds = page.getContent().stream()
                .map(OrgInfoMessage::getAuthorUserId)
                .distinct()
                .toList();
        Map<Long, User> usersById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return page.map(m -> toDTO(m, usersById.get(m.getAuthorUserId())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isSectionMember(#sectionId)")
    public Page<OrgInfoMessageDTO> getSectionMessages(Long sectionId, Pageable pageable) {
        Page<OrgInfoMessage> page = orgInfoMessageRepository
                .findBySectionIdOrderByCreatedAtDesc(sectionId, pageable);

        List<Long> authorIds = page.getContent().stream()
                .map(OrgInfoMessage::getAuthorUserId)
                .distinct()
                .toList();
        Map<Long, User> usersById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return page.map(m -> toDTO(m, usersById.get(m.getAuthorUserId())));
    }

    private void sendNotificationsToUsers(List<Long> userIds, String text, Long messageId) {
        if (userIds.isEmpty()) {
            return;
        }
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {
            try {
                sendNotificationToUser(user, text, messageId);
            } catch (Exception e) {
                log.error("Failed to send info message notification to user {}", user.getId(), e);
            }
        }
    }

    private void sendNotificationToUser(User user, String text, Long messageId) {
        NotificationChannelType channel = user.getNotificationChannel();
        NotificationMessage msg = new NotificationMessage(
                user.getId(),
                channel,
                "ORG_INFO_MESSAGE",
                text,
                Map.of("message_id", messageId)
        );
        try {
            String json = objectMapper.writeValueAsString(msg);
            if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
                rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
            } else if (channel == NotificationChannelType.VK && user.getVkUserId() != null) {
                rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
            } else {
                rabbitTemplate.convertAndSend(emailQueueName, json);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize notification message", e);
        }

        try {
            webSocketNotificationService.send(user.getId(), InAppNotificationDTO.builder()
                    .type(InAppNotificationType.NEW_INFO_MESSAGE)
                    .title("New info message")
                    .body(text)
                    .entityId(messageId)
                    .entityType("ORG_INFO_MESSAGE")
                    .build());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for info message {} to user {}", messageId, user.getId(), e);
        }
    }

    private OrgInfoMessageDTO toDTO(OrgInfoMessage message, User author) {
        return OrgInfoMessageDTO.builder()
                .id(message.getId())
                .organizationId(message.getOrganizationId())
                .sectionId(message.getSectionId())
                .authorUserId(message.getAuthorUserId())
                .authorName(author != null ? author.getName() : null)
                .text(message.getText())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
