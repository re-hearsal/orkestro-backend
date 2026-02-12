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
public class TaskVisibilityUpdateRequestDTO {

    @NotNull
    private TaskVisibility visibility;

    @Size(max = 50)
    private List<@NotNull @Positive Long> visibilityRoleIds;
}
