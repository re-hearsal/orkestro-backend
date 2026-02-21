package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.Event;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findByOrganizationId(Long organizationId);

    List<Event> findByOrganizationIdAndStartTimeBetween(Long organizationId, Instant from, Instant to);

    List<Event> findByRemindBeforeMinutesIsNotNull();

    @Query("""
            select distinct tag
            from Event e
            join e.tags tag
            where e.organizationId = :organizationId
            order by tag
            """)
    List<String> findDistinctTagsByOrganizationId(@Param("organizationId") Long organizationId);
}
