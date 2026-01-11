package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalRoleDTO {

    private Long id;
    private RoleScopeType scope;
    private Long organizationId;
    private Long sectionId;
    private String name;
    private boolean system;
}
