package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.Event;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByOrganizationId(Long organizationId);

    List<Event> findByOrganizationIdAndStartTimeBetween(Long organizationId, Instant from, Instant to);

    List<Event> findByRemindBeforeMinutesIsNotNull();
}
