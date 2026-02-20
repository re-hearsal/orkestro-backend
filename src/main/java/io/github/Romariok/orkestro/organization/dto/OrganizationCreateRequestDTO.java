package io.github.Romariok.orkestro.organization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationCreateRequestDTO {

   @NotBlank
   @Size(min = 3, max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Name cannot be blank")
   private String name;

   @NotBlank
   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Location cannot be blank")
   private String location;

   @Size(max = 1000)
   @Pattern(regexp = ".*\\S.*", message = "Description cannot be blank")
   private String description;

   private MultipartFile profileImage;

   @NotNull
   private VisibilityLevelType visibilityLevel;

   @Size(max = 100)
   private List<@Valid @NotNull OrganizationLinkDTO> links;

   @AssertTrue(message = "Profile image must be a non-empty image")
   private boolean isProfileImageValid() {
      if (profileImage == null) {
         return true;
      }
      if (profileImage.isEmpty() || profileImage.getSize() <= 0) {
         return false;
      }
      String contentType = profileImage.getContentType();
      return contentType != null && !contentType.isBlank() && contentType.startsWith("image/");
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
          String key = link.getLinkType().name() + "|" + link.getUrl().trim().toLowerCase(Locale.ROOT);
         if (!seen.add(key)) {
            return false;
         }
      }
      return true;
   }
}
