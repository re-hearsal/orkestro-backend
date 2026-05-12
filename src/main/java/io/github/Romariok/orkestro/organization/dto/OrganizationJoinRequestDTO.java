package io.github.Romariok.orkestro.organization.dto;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationJoinRequestDTO {

   private Long userId;
   private String username;
   private String name;
   private Long profileImageFileId;
   private OrganizationUserStatusType status;
   private Instant joinedAt;
   private String description;

   public static OrganizationJoinRequestDTO fromEntity(OrganizationUser ou) {
      if (ou == null) {
         return null;
      }

      String username = null;
      String name = null;
      Long profileImageFileId = null;

      if (ou.getUser() != null) {
         username = ou.getUser().getUsername();
         name = ou.getUser().getName();
         profileImageFileId = ou.getUser().getProfileImageFileId();
      }

      return new OrganizationJoinRequestDTO(
            ou.getUserId(),
            username,
            name,
            profileImageFileId,
            ou.getStatus(),
            ou.getJoinedAt(),
            ou.getDescription());
   }
}
