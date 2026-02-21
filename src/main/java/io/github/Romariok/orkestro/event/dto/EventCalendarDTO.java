package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventCalendarDTO {
    private Long id;
    private Long organizationId;
    private String title;
    private EventType eventType;
    private String location;
    private List<String> tags;
    private Instant startTime;
    private Instant endTime;
}
