package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.models.Role;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TechnicalRoleMapper {

    TechnicalRoleDTO toDto(Role role);

    List<TechnicalRoleDTO> toDtoList(List<Role> roles);
}


