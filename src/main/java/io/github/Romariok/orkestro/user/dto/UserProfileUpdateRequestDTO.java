package io.github.Romariok.orkestro.user.dto;

import java.time.LocalDate;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
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
