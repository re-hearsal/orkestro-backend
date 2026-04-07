package io.github.Romariok.orkestro.organization.dto;

import java.time.Instant;
import java.util.List;

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
   private List<OrganizationLinkDTO> links;
}
