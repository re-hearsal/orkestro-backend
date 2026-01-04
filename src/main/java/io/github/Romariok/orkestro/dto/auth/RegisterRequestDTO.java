package io.github.Romariok.orkestro.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

   @NotBlank
   private String username;

   @NotBlank
   private String password;

   @NotBlank
   private String name;

   @Email
   @NotBlank
   private String email;
}
