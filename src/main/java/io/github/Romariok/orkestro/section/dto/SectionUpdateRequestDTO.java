package io.github.Romariok.orkestro.section.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionUpdateRequestDTO {

    private String name;
    private String description;
}
