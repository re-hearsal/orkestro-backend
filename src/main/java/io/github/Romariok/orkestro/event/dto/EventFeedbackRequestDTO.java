package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFeedbackRequestDTO {
    private String title;
    private EventType eventType;
    private Instant from;
    private Instant to;
    private List<String> tags;
    /** Allowed values: commentCreatedAt, eventStartTime, rating */
    private String sortField;
}
