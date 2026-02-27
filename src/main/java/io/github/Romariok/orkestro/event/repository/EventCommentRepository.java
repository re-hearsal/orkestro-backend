package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventComment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, Long> {

    long countByEventId(Long eventId);

    List<EventComment> findByEventIdInOrderByCreatedAtDesc(Collection<Long> eventIds);
}
