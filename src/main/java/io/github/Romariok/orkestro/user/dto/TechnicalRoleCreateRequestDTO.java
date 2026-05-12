package io.github.Romariok.orkestro.user.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalRoleCreateRequestDTO {

    @NotBlank
    private String name;

    private List<String> permissionCodes;
}
