package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class EventCreateRequestDTO {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private EventType eventType;

    private String externalLink;

    private String location;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    private List<Long> participantUserIds;

    private List<Long> participantSectionIds;

    private Boolean includeAllOrganizationMembers;

    private List<Long> fileIds;

    private List<Long> songIds;

    private List<String> tags;

    private Boolean sendRsvp;

    private Integer remindBeforeMinutes;
}
