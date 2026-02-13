package io.github.Romariok.orkestro.organization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUpdateRequestDTO {

   @Size(min = 3, max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Name cannot be blank")
   private String name;

   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Location cannot be blank")
   private String location;

   @Size(max = 1000)
   @Pattern(regexp = ".*\\S.*", message = "Description cannot be blank")
   private String description;

   @Positive
   private Long profileImageFileId;

   /**
    * Если не null — полностью заменить список ссылок.
    * Пустой список означает удаление всех ссылок.
    */
   @Size(max = 100)
   private List<@Valid @NotNull OrganizationLinkDTO> links;

   @AssertTrue(message = "At least one field must be provided")
   private boolean isAnyFieldProvided() {
      return name != null
            || location != null
            || description != null
            || profileImageFileId != null
            || links != null;
   }

   @AssertTrue(message = "Links must not contain duplicates")
   private boolean isLinksUnique() {
      if (links == null || links.isEmpty()) {
         return true;
      }
      Set<String> seen = new HashSet<>();
      for (OrganizationLinkDTO link : links) {
         if (link == null || link.getLinkType() == null || link.getUrl() == null) {
            continue;
         }
         String key = link.getLinkType().name() + "|" + link.getUrl().trim().toLowerCase();
         if (!seen.add(key)) {
            return false;
         }
      }
      return true;
   }
}
