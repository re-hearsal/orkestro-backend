package io.github.Romariok.orkestro.user.mapper;

import io.github.Romariok.orkestro.user.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.user.models.Instrument;

import java.util.List;

/**
 * Mapper for converting {@link Instrument} entities to {@link MusicalRoleDTO}.
 *
 * <p>
 * Реализация предоставляется вручную в {@code MusicalRoleMapperImpl} как
 * Spring-бин.
 * </p>
 */
public interface MusicalRoleMapper {

    MusicalRoleDTO toDto(Instrument instrument);

    List<MusicalRoleDTO> toDtoList(List<Instrument> instruments);
}
