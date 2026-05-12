package io.github.Romariok.orkestro.event.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCommentDTO {
    private Long id;
    private Long authorUserId;
    private String authorName;
    private Long authorProfileImageFileId;
    private String text;
    private Integer rating;
    private Instant createdAt;
}
