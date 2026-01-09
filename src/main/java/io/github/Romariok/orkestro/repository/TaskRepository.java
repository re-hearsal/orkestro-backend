package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.enums.TaskStatus;
import io.github.Romariok.orkestro.models.task.Task;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findBySectionId(Long sectionId);

    List<Task> findByOrganizationId(Long organizationId);

    List<Task> findByOrganizationIdAndStatusIn(Long organizationId, Collection<TaskStatus> statuses);

    List<Task> findBySectionIdAndStatusIn(Long sectionId, Collection<TaskStatus> statuses);
}
