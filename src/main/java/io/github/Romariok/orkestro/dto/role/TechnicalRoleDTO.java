package io.github.Romariok.orkestro.dto.role;

import io.github.Romariok.orkestro.models.enums.RoleScopeType;
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


