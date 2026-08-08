package io.github.Romariok.orkestro.notification.dto;

import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotificationDTO {

    private Long id;
    private InAppNotificationType type;
    private String title;
    private String body;
    private Long entityId;
    private String entityType;
    private Long organizationId;
    private Long sectionId;
    private Boolean isRead;
    private Instant createdAt;
}
