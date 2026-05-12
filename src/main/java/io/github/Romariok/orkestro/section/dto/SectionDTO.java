package io.github.Romariok.orkestro.section.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionDTO {

    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private Long parentSectionId;
}
