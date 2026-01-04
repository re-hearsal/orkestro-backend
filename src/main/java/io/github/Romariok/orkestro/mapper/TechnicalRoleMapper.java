package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.role.TechnicalRoleDTO;
import io.github.Romariok.orkestro.models.role.Role;

import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TechnicalRoleMapper {

    TechnicalRoleDTO toDto(Role role);

    List<TechnicalRoleDTO> toDtoList(List<Role> roles);
}


