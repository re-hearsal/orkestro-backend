package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.task.TaskDTO;
import io.github.Romariok.orkestro.models.task.Task;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    TaskDTO toDto(Task task);

    List<TaskDTO> toDtoList(List<Task> tasks);
}
