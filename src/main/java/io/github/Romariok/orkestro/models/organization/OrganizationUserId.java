package io.github.Romariok.orkestro.models.organization;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class OrganizationUserId implements Serializable {

   private static final long serialVersionUID = 1L;

   private Long organizationId;
   private Long userId;
}
