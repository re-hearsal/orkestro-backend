package io.github.Romariok.orkestro.task.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.task.models.TaskVisibilityRole;
import io.github.Romariok.orkestro.task.models.TaskVisibilityRoleId;

@Repository
public interface TaskVisibilityRoleRepository extends JpaRepository<TaskVisibilityRole, TaskVisibilityRoleId> {

    List<TaskVisibilityRole> findByTaskId(Long taskId);

    List<TaskVisibilityRole> findByTaskIdIn(Collection<Long> taskIds);

    void deleteByTaskId(Long taskId);
}
