package io.github.Romariok.orkestro.task.dto;

import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(min = 1, max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    private TaskVisibility visibility;

    /**
     * Если не null — полностью заменить список ролей, которые могут видеть задачу.
     */
    @Size(max = 50)
    private List<@NotNull @Positive Long> visibilityRoleIds;

    /**
     * Если не null — полностью заменить список файлов.
     */
    @Size(max = 50)
    private List<@NotNull @Positive Long> fileIds;
}
