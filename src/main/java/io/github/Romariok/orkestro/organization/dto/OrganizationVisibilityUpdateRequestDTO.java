package io.github.Romariok.orkestro.organization.dto;

import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationVisibilityUpdateRequestDTO {

   @NotNull
   private VisibilityLevelType visibilityLevel;
}
