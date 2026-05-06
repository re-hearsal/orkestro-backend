package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.time.Instant;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventFeedbackRowDTO {
    private Long commentId;
    private String commentText;
    private Integer rating;
    private Instant commentCreatedAt;
    private Long authorUserId;
    private String authorName;
    private Long authorProfileImageFileId;
    private Long eventId;
    private String eventTitle;
    private EventType eventType;
    private Instant eventStartTime;
    private Instant eventEndTime;
    private Set<String> eventTags;
}
