package io.github.Romariok.orkestro.event.dto;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDescriptionTemplateCreateRequestDTO {

    @Schema(example = "Стандартное описание репетиции")
    @NotBlank
    private String title;

    @Schema(example = "REHEARSAL")
    @NotNull
    private EventType eventType;

    @Schema(example = "На данной репетиции мы отрабатываем...")
    @NotBlank
    private String content;
}
