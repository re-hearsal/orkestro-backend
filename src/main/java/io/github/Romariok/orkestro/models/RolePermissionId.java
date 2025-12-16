package io.github.Romariok.orkestro.models;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RolePermissionId implements Serializable {

    private Long roleId;
    private String permissionCode;
}


