package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
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
    private List<Long> participantSectionIds;

    /**
     * Если не null — при пересчёте участников учитывать или не учитывать
     * всех пользователей организации как участников.
     */
    private Boolean includeAllOrganizationMembers;

    /**
     * Если не null — полностью заменить список файлов события.
     */
    private List<Long> fileIds;

    /**
     * Если не null — полностью заменить список песен события.
     */
    private List<Long> songIds;

    private List<String> tags;
}
