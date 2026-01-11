package io.github.Romariok.orkestro.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreateRequestDTO {

   @NotBlank
   private String name;

   @NotBlank
   private String location;

   private String description;

   @NotNull
   private Long profileImageFileId;

   @NotNull
   private VisibilityLevelType visibilityLevel;

   private List<OrganizationLinkDTO> links;
}
