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
   private OrganizationUserStatusType status;
   private Instant joinedAt;

   public static OrganizationJoinRequestDTO fromEntity(OrganizationUser ou) {
      if (ou == null) {
         return null;
      }
      return new OrganizationJoinRequestDTO(
            ou.getUserId(),
            ou.getStatus(),
            ou.getJoinedAt());
   }
}

