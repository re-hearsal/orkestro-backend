package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventUpdateRequestDTO {

    @Size(max = 30)
    private String title;

    @Size(max = 2000)
    private String description;

    private EventType eventType;
    private String externalLink;
    private String location;
    @FutureOrPresent
    private Instant startTime;
    private Instant endTime;

    private List<Long> participantUserIds;

    @Schema(description = "IDs of sections applicable to this event", example = "[10, 11]")
    private List<Long> participantSectionIds;

    @Schema(description = "If true, event applies to whole organization and section IDs are ignored", example = "false")
    private Boolean includeAllOrganizationMembers;

    @Min(0)
    @Max(1440)
    private Integer remindBeforeMinutes;

    @Size(max = 50)
    private List<Long> songIds;

    @Size(max = 5)
    private List<String> tags;
}
