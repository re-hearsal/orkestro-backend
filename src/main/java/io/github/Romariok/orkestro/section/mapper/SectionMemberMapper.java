package io.github.Romariok.orkestro.section.mapper;

import io.github.Romariok.orkestro.section.dto.SectionMemberDTO;
import io.github.Romariok.orkestro.section.models.SectionUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SectionMemberMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "profileImageFileId", source = "user.profileImageFileId")
    @Mapping(target = "joinedAt", source = "joinedAt")
    SectionMemberDTO toDto(SectionUser membership);
}

