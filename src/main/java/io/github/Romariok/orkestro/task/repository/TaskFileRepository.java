package io.github.Romariok.orkestro.task.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.task.models.TaskFile;
import io.github.Romariok.orkestro.task.models.TaskFileId;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, TaskFileId> {

    List<TaskFile> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);

    boolean existsByTaskIdAndFileId(Long taskId, Long fileId);

    void deleteByTaskIdAndFileId(Long taskId, Long fileId);
}
