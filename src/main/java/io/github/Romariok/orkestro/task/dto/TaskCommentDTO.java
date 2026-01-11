package io.github.Romariok.orkestro.task.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCommentDTO {

    private Long id;
    private Long taskId;
    private Long authorUserId;
    private String text;
    private Instant createdAt;
}
