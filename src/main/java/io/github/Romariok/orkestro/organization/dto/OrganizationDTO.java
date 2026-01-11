package io.github.Romariok.orkestro.organization.dto;

import java.time.Instant;
import java.util.List;

import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {

   private Long id;
   private String name;
   private String location;
   private String description;
   private Long profileImageFileId;
   private Instant createdAt;
   private VisibilityLevelType visibilityLevel;

   private List<OrganizationLinkDTO> links;
}
