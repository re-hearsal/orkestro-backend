package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.models.Instrument;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MusicalRoleMapper {

    @Mapping(target = "instrumentId", source = "id")
    @Mapping(target = "instrumentName", source = "name")
    MusicalRoleDTO toDto(Instrument instrument);

    List<MusicalRoleDTO> toDtoList(List<Instrument> instruments);
}


