package io.github.Romariok.orkestro.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberDTO {
   private Long id;
   private String username;
   private String name;
   private Long profileImageFileId;
}

