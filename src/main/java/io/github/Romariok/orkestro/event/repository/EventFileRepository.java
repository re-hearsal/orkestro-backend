package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventFile;
import io.github.Romariok.orkestro.event.models.EventFileId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventFileRepository extends JpaRepository<EventFile, EventFileId> {

    List<EventFile> findByEventId(Long eventId);

    List<EventFile> findByEventIdIn(List<Long> eventIds);

    void deleteByEventId(Long eventId);

    boolean existsByEventIdAndFileId(Long eventId, Long fileId);

    void deleteByEventIdAndFileId(Long eventId, Long fileId);

    boolean existsByFileId(Long fileId);
}
