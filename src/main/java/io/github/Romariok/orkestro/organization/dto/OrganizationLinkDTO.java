package io.github.Romariok.orkestro.organization.dto;

import io.github.Romariok.orkestro.organization.models.enums.LinkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLinkDTO {

   @NotNull
   private LinkType linkType;

   @NotBlank
   @Size(max = 2048)
   @Pattern(regexp = "^https?://.+", message = "Url must start with http:// or https://")
   private String url;
}
