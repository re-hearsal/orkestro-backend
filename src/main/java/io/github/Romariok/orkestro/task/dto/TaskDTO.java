package io.github.Romariok.orkestro.task.dto;

import java.time.Instant;
import java.util.List;

import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDTO {

    private Long id;
    private Long organizationId;
    private Long sectionId;
    private String title;
    private String description;
    private Long authorUserId;
    private Long assigneeUserId;
    private TaskStatus status;
    private TaskVisibility visibility;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    private List<Long> fileIds;
    private List<TaskCommentDTO> comments;
    private List<Long> visibilityRoleIds;
}
