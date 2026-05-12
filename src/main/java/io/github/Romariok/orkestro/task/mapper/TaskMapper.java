package io.github.Romariok.orkestro.task.mapper;

import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.models.Task;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    TaskDTO toDto(Task task);

    List<TaskDTO> toDtoList(List<Task> tasks);
}
