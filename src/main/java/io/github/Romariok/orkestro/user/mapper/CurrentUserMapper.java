package io.github.Romariok.orkestro.user.mapper;

import io.github.Romariok.orkestro.user.dto.CurrentUserResponseDTO;
import io.github.Romariok.orkestro.user.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurrentUserMapper {

   CurrentUserResponseDTO toDto(User user);
}
