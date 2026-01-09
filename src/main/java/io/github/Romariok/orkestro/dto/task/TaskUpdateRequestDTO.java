package io.github.Romariok.orkestro.dto.task;

import io.github.Romariok.orkestro.models.enums.TaskVisibility;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUpdateRequestDTO {

    private String title;
    private String description;
    private Long assigneeUserId;
    private TaskVisibility visibility;

    /**
     * Если не null — полностью заменить список ролей, которые могут видеть задачу.
     */
    private List<Long> visibilityRoleIds;

    /**
     * Если не null — полностью заменить список файлов.
     */
    private List<Long> fileIds;
}
