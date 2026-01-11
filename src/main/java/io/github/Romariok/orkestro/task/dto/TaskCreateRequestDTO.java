package io.github.Romariok.orkestro.task.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCreateRequestDTO {

    @NotBlank
    private String title;

    private String description;
    private Long assigneeUserId;
    private TaskVisibility visibility;

    /**
     * Идентификаторы ролей, которые могут видеть задачу, если visibility =
     * ROLE_RESTRICTED.
     */
    private List<Long> visibilityRoleIds;

    /**
     * Идентификаторы файлов, прикрепляемых к задаче.
     */
    private List<Long> fileIds;
}
