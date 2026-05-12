package io.github.Romariok.orkestro.organization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Запрос на вступление в организацию")
public class OrganizationJoinCreateRequestDTO {

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(
            description = "Описание заявки на вступление",
            example = "Хочу присоединиться как саксофонист, есть концертный опыт 5 лет.",
            maxLength = 1000)
    private String description;
}
