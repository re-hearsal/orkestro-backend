package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserResponseDTO {

   private Long id;
   private String username;
   private String name;
   private String email;
   private String location;
   private LocalDate birthDate;
   private NotificationChannelType notificationChannel;
   private UserLanguageType preferredLanguage;
   private Long profileImageFileId;
   private Long telegramUserId;
}
