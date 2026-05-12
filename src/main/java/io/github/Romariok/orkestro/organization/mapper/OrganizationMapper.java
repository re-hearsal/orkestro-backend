package io.github.Romariok.orkestro.organization.mapper;

import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.models.Organization;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper {

   OrganizationDTO toDto(Organization organization);

   List<OrganizationDTO> toDtoList(List<Organization> organizations);
}
