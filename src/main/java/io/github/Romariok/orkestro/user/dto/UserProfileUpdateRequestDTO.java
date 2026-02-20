package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequestDTO {

   private String name;

   @Email
   @Size(max = 255)
   private String email;

   @Size(max = 255)
   private String location;
   private LocalDate birthDate;
   private UserLanguageType preferredLanguage;

   @AssertTrue(message = "At least one field must be provided")
   private boolean isAnyFieldProvided() {
      return name != null
            || email != null
            || location != null
            || birthDate != null
            || preferredLanguage != null;
   }

   @AssertTrue(message = "Name cannot be blank")
   private boolean isNameValid() {
      return name == null || name.matches(".*\\S.*");
   }

   @AssertTrue(message = "Email cannot be blank")
   private boolean isEmailValid() {
      return email == null || email.matches(".*\\S.*");
   }

   @AssertTrue(message = "Location cannot be blank")
   private boolean isLocationValid() {
      return location == null || location.matches(".*\\S.*");
   }
}
