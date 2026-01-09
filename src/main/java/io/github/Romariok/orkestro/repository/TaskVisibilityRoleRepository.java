package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.task.TaskVisibilityRole;
import io.github.Romariok.orkestro.models.task.TaskVisibilityRoleId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskVisibilityRoleRepository extends JpaRepository<TaskVisibilityRole, TaskVisibilityRoleId> {

    List<TaskVisibilityRole> findByTaskId(Long taskId);

    List<TaskVisibilityRole> findByTaskIdIn(Collection<Long> taskIds);

    void deleteByTaskId(Long taskId);
}
