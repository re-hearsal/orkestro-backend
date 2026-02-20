package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventSection;
import io.github.Romariok.orkestro.event.models.EventSectionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSectionRepository extends JpaRepository<EventSection, EventSectionId> {

    List<EventSection> findByEventId(Long eventId);

    List<EventSection> findByEventIdIn(Collection<Long> eventIds);

    void deleteByEventId(Long eventId);
}
