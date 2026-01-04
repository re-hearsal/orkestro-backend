package io.github.Romariok.orkestro.dto.organization;

import io.github.Romariok.orkestro.models.enums.VisibilityLevelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
