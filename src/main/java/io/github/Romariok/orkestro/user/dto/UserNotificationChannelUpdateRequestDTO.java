package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationChannelUpdateRequestDTO {

   @NotNull
   private NotificationChannelType notificationChannel;
}
