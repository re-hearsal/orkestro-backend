package io.github.Romariok.orkestro.event.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCommentsByEventDTO {
    private Long eventId;
    private long commentsCount;
    private List<EventCommentDTO> comments;
}
