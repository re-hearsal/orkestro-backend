package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventParticipantId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, EventParticipantId> {

    List<EventParticipant> findByEventId(Long eventId);

    List<EventParticipant> findByEventIdIn(Collection<Long> eventIds);

    void deleteByEventId(Long eventId);

    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    List<EventParticipant> findByUserId(Long userId);
}
