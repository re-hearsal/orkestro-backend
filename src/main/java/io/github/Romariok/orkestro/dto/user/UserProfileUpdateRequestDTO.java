package io.github.Romariok.orkestro.dto.user;

import io.github.Romariok.orkestro.models.enums.NotificationChannelType;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequestDTO {

   private String name;
   private String email;
   private String location;
   private LocalDate birthDate;
   private NotificationChannelType notificationChannel;
}


