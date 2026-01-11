package io.github.Romariok.orkestro.section.mapper;

import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.models.Section;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SectionMapper {

    SectionDTO toDto(Section section);

    List<SectionDTO> toDtoList(List<Section> sections);
}
