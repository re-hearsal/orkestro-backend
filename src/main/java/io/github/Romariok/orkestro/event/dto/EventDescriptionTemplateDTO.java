package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDescriptionTemplateDTO {
    private Long id;
    private Long organizationId;
    private EventType eventType;
    private String title;
    private String content;
    private Long createdByUserId;
    private Instant createdAt;
}
