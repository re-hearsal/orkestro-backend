package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

   @NotBlank
   @Size(min = 3, max = 50)
   private String username;

   @NotBlank
   @Size(min = 8, max = 100)
   private String password;

   @NotBlank
   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Name cannot be blank")
   private String name;

   @Email
   @NotBlank
   @Size(max = 255)
   private String email;

   @Size(max = 255)
   private String location;

   @PastOrPresent(message = "Birth date must not be in the future")
   private LocalDate birthDate;

   private UserLanguageType preferredLanguage;

   private MultipartFile avatar;

   @AssertTrue(message = "Avatar file must be a non-empty image")
   private boolean isAvatarValid() {
      if (avatar == null) {
         return true;
      }
      if (avatar.isEmpty() || avatar.getSize() <= 0) {
         return false;
      }
      String contentType = avatar.getContentType();
      return contentType != null && !contentType.isBlank() && contentType.startsWith("image/");
   }
}
