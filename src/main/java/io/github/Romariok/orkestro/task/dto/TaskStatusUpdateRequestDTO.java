package io.github.Romariok.orkestro.task.dto;

import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatusUpdateRequestDTO {

    @NotNull
    private TaskStatus status;
}
