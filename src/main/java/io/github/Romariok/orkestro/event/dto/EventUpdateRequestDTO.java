package io.github.Romariok.orkestro.event.dto;

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
public class EventUpdateRequestDTO {

    private String title;
    private String description;
    private EventType eventType;
    private String externalLink;
    private String location;
    private Instant startTime;
    private Instant endTime;

    /**
     * Полностью заменить набор участников, заданных по пользователям.
     */
    private List<Long> participantUserIds;

    /**
     * Полностью заменить набор участников, заданных по секциям.
     */
    @Schema(description = "IDs of sections applicable to this event", example = "[10, 11]")
    private List<Long> participantSectionIds;

    /**
     * Если не null — при пересчёте участников учитывать или не учитывать
     * всех пользователей организации как участников.
     */
    @Schema(description = "If true, event applies to whole organization and section IDs are ignored", example = "false")
    private Boolean includeAllOrganizationMembers;

    private Integer remindBeforeMinutes;

    /**
     * Если не null — полностью заменить список песен события.
     */
    private List<Long> songIds;

    private List<String> tags;
}
