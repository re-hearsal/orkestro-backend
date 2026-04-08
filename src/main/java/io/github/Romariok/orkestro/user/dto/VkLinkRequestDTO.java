package io.github.Romariok.orkestro.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VkLinkRequestDTO {

    @NotBlank
    private String token;

    @NotNull
    private Long vkUserId;
}
