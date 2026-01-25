package io.github.Romariok.orkestro.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestDTO {

   @NotBlank
   private String username;

   @NotBlank
   private String newPassword;
}
