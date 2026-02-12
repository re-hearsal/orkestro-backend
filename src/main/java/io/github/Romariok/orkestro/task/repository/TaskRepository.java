package io.github.Romariok.orkestro.task.repository;

import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findBySectionId(Long sectionId);

    List<Task> findByOrganizationId(Long organizationId);

    List<Task> findByOrganizationIdAndStatusIn(Long organizationId, Collection<TaskStatus> statuses);

    List<Task> findBySectionIdAndStatusIn(Long sectionId, Collection<TaskStatus> statuses);

    Page<Task> findByOrganizationIdAndStatusIn(
            Long organizationId, Collection<TaskStatus> statuses, Pageable pageable);
}
