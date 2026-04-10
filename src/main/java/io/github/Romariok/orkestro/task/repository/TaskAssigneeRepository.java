package io.github.Romariok.orkestro.task.repository;

import io.github.Romariok.orkestro.task.models.TaskAssignee;
import io.github.Romariok.orkestro.task.models.TaskAssigneeId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, TaskAssigneeId> {

    List<TaskAssignee> findByTaskId(Long taskId);

    List<TaskAssignee> findByTaskIdIn(Collection<Long> taskIds);

    void deleteByTaskIdAndUserId(Long taskId, Long userId);

    boolean existsByTaskIdAndUserId(Long taskId, Long userId);
}
