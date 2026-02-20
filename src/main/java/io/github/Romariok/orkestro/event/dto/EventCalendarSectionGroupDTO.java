package io.github.Romariok.orkestro.event.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventCalendarSectionGroupDTO {
    private Long sectionId;
    private List<EventCalendarDTO> events;
}
