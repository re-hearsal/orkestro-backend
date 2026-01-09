package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.task.TaskFile;
import io.github.Romariok.orkestro.models.task.TaskFileId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, TaskFileId> {

    List<TaskFile> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}
