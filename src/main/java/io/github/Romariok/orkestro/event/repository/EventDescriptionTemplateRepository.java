package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventDescriptionTemplate;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDescriptionTemplateRepository extends JpaRepository<EventDescriptionTemplate, Long> {

    List<EventDescriptionTemplate> findByOrganizationId(Long organizationId);

    List<EventDescriptionTemplate> findByOrganizationIdAndEventType(Long organizationId, EventType eventType);
}
