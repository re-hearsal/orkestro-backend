package io.github.Romariok.orkestro.notification.controller;

import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.InAppNotification;
import io.github.Romariok.orkestro.notification.repository.InAppNotificationRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification management")
public class NotificationController {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get in-app notifications for current user",
               description = "Returns a paginated list of in-app notifications for the authenticated user, newest first")
    @ApiResponse(responseCode = "200", description = "Page of notifications returned")
    public Page<InAppNotificationDTO> getNotifications(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found or does not belong to current user")
    public ResponseEntity<InAppNotificationDTO> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        InAppNotification notification = inAppNotificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + id));
        notification.setIsRead(true);
        InAppNotification saved = inAppNotificationRepository.save(notification);
        return ResponseEntity.ok(toDTO(saved));
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
