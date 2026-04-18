package io.github.Romariok.orkestro.notification.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.notification.models.InAppNotification;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.notification.repository.InAppNotificationRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private NotificationController notificationController;

    private InAppNotification buildNotification(Long id, Long userId) {
        return InAppNotification.builder()
                .id(id)
                .userId(userId)
                .type(InAppNotificationType.NEW_TASK)
                .title("New task assigned")
                .body("You have a new task")
                .isRead(false)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @Test
    void getNotifications_returnsPage() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        InAppNotification notification = buildNotification(10L, 1L);
        Page<InAppNotification> notifPage = new PageImpl<>(List.of(notification));
        when(inAppNotificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any()))
                .thenReturn(notifPage);

        var result = notificationController.getNotifications(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("New task assigned", result.getContent().get(0).getTitle());
        assertEquals(false, result.getContent().get(0).getIsRead());
    }

    @Test
    void markAsRead_returnsOk() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        InAppNotification notification = buildNotification(10L, 1L);
        when(inAppNotificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(inAppNotificationRepository.save(notification)).thenReturn(notification);

        var result = notificationController.markAsRead(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertTrue(result.getBody().getIsRead());
    }

    @Test
    void markAsRead_notFound_throwsException() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(inAppNotificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> notificationController.markAsRead(99L));
    }

    @Test
    void markAsRead_wrongUser_throwsException() {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);
        InAppNotification notification = buildNotification(10L, 1L);
        when(inAppNotificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(EntityNotFoundException.class,
                () -> notificationController.markAsRead(10L));
    }
}
