package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.task.TaskComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {

    List<TaskComment> findByTaskId(Long taskId);
}
