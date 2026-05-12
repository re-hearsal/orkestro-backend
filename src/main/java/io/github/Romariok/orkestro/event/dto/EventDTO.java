package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class EventDTO {

    private Long id;
    private Long organizationId;
    private Long createdByUserId;
    private String title;
    private String description;
    private EventType eventType;
    private String externalLink;
    private String location;
    private Instant startTime;
    private Instant endTime;
    private Instant createdAt;

    private List<String> tags;

    private List<Long> participantUserIds;

    private List<Long> participantSectionIds;

    @Schema(description = "Whether this event applies to all organization members")
    private Boolean includeAllOrganizationMembers;

    private Integer remindBeforeMinutes;

    @Schema(description = "RSVP status of the currently authenticated user for this event. Null if user is not a participant.")
    private EventRsvpStatus myRsvpStatus;

    private List<Long> fileIds;

    private List<Long> songIds;
}
