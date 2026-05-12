package io.github.Romariok.orkestro.event.mapper;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.models.Event;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {

    EventDTO toDto(Event event);

    List<EventDTO> toDtoList(List<Event> events);
}
