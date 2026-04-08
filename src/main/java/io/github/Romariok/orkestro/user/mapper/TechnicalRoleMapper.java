package io.github.Romariok.orkestro.user.mapper;

import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.models.Role;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TechnicalRoleMapper {

    @Mapping(target = "permissionCodes", ignore = true)
    TechnicalRoleDTO toDto(Role role);

    @Mapping(target = "permissionCodes", ignore = true)
    List<TechnicalRoleDTO> toDtoList(List<Role> roles);
}
