package io.github.Romariok.orkestro.organization.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationUpdateRequestDTO {

   private String name;
   private String location;
   private String description;
   private Long profileImageFileId;

   /**
    * Если не null — полностью заменить список ссылок.
    * Пустой список означает удаление всех ссылок.
    */
   private List<OrganizationLinkDTO> links;
}
