package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateRequestDTO {

    @Schema(example = "New Year Concert")
    @NotBlank
    private String title;

    @Schema(example = "Annual New Year concert")
    private String description;

    @Schema(example = "CONCERT")
    @NotNull
    private EventType eventType;

    private String externalLink;

    private String location;

    @Schema(example = "2030-01-01T10:00:00Z")
    @NotNull
    private Instant startTime;

    @Schema(example = "2030-01-01T12:00:00Z")
    @NotNull
    private Instant endTime;

    private List<Long> participantUserIds;

    @Schema(description = "IDs of sections applicable to this event", example = "[10, 11]")
    private List<Long> participantSectionIds;

    @Schema(description = "If true, event applies to whole organization and section IDs are ignored", example = "false")
    private Boolean includeAllOrganizationMembers;

    @Size(max = 50)
    private List<MultipartFile> files;

    private List<Long> songIds;

    private List<String> tags;

    private Boolean sendRsvp;

    private Integer remindBeforeMinutes;

    @Schema(description = "ID шаблона описания. Если указан, поле description будет заполнено из шаблона.")
    private Long descriptionTemplateId;
}
