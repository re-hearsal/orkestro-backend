package io.github.Romariok.orkestro.organization.dto;

import io.github.Romariok.orkestro.organization.models.enums.LinkType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationLinkDTO {

   private LinkType linkType;
   private String url;
}
