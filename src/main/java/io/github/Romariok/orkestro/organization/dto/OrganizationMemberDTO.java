package io.github.Romariok.orkestro.organization.dto;

import java.time.Instant;
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
   private Instant joinedAt;
   private RoleInfo role;

   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public static class RoleInfo {
      private Long id;
      private String name;
   }
}
