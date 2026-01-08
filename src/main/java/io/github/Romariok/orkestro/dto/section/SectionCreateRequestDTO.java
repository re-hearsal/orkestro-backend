package io.github.Romariok.orkestro.dto.section;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionCreateRequestDTO {

    @NotBlank
    private String name;

    private String description;
}
