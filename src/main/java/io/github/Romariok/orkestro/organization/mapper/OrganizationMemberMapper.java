package io.github.Romariok.orkestro.organization.mapper;

import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.user.models.User;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMemberMapper {

    @Mapping(target = "joinedAt", source = "joinedAt")
    OrganizationMemberDTO toDto(User user, Instant joinedAt);
}
