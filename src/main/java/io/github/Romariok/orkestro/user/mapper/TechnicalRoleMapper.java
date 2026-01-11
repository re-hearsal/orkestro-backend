package io.github.Romariok.orkestro.user.mapper;

import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.models.Role;

import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TechnicalRoleMapper {

    TechnicalRoleDTO toDto(Role role);

    List<TechnicalRoleDTO> toDtoList(List<Role> roles);
}
