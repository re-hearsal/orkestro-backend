package io.github.Romariok.orkestro.notification;

import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.InAppNotification;
import io.github.Romariok.orkestro.notification.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void sendToTopic(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.error("Failed to push WebSocket message to topic {}", destination, ex);
        }
    }

    public void send(Long userId, InAppNotificationDTO dto) {
        try {
            InAppNotification entity = InAppNotification.builder()
                    .userId(userId)
                    .type(dto.getType())
                    .title(dto.getTitle())
                    .body(dto.getBody())
                    .entityId(dto.getEntityId())
                    .entityType(dto.getEntityType())
                    .isRead(false)
                    .build();
            InAppNotification saved = inAppNotificationRepository.save(entity);

            InAppNotificationDTO savedDto = toDTO(saved);
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(userId),
                        "/queue/notifications",
                        savedDto
                );
            } catch (Exception ex) {
                log.error("Failed to push WebSocket notification to user {}", userId, ex);
            }
        } catch (Exception ex) {
            log.error("Failed to persist in-app notification for user {}", userId, ex);
        }
    }

    private InAppNotificationDTO toDTO(InAppNotification n) {
        return InAppNotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .entityId(n.getEntityId())
                .entityType(n.getEntityType())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
