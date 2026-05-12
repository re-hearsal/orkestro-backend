package io.github.Romariok.orkestro.event.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventCalendarGroupedResponseDTO {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    private List<EventCalendarSectionGroupDTO> sectionGroups;
    private List<EventCalendarDTO> organizationWideEvents;
}
