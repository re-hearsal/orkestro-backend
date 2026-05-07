package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTelegramLinkService {

   private final UserRepository userRepository;
   private final UserTelegramLinkTokenService tokenService;
   private final SecurityUtils securityUtils;
   private final RabbitTemplate rabbitTemplate;
   private final ObjectMapper objectMapper;

   @Value("${orkestro.vk.bot-message-queue-name:vk_notification_queue}")
   private String vkBotMessageQueueName;

   @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
   private String telegramBotMessageQueueName;

   @Transactional
   public String createLinkTokenForCurrentUser() {
      Long currentUserId = securityUtils.getCurrentUserId();
      return createLinkTokenForUser(currentUserId);
   }

   @Transactional
   public String createLinkTokenForUser(Long userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      String token = tokenService.createToken(user.getId());

      log.info("Created Telegram link token for user id={}", userId);
      return token;
   }


   @Transactional
   public void linkTelegram(Long userId, Long telegramUserId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      if (user.getVkUserId() != null) {
         sendVkUnlinkNotification(user.getVkUserId());
         user.setVkUserId(null);
      }

      user.setTelegramUserId(telegramUserId);
      user.setNotificationChannel(NotificationChannelType.TELEGRAM);
      userRepository.save(user);

      log.info("Linked Telegram user {} to user id={}", telegramUserId, userId);
   }


   @Transactional
   public void unlinkTelegram(Long userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      if (user.getTelegramUserId() != null) {
         sendTelegramUnlinkNotification(user.getTelegramUserId());
         user.setTelegramUserId(null);
      }
      user.setNotificationChannel(NotificationChannelType.EMAIL);
      userRepository.save(user);

      log.info("Unlinked Telegram from user id={}", userId);
   }

   private void sendVkUnlinkNotification(Long vkUserId) {
      try {
         Map<String, Object> payload = Map.of(
               "vk_user_id", vkUserId,
               "text", "Ваш аккаунт ВКонтакте был отвязан, так как вы подключили Telegram.",
               "type", "vk.unlink");
         String json = objectMapper.writeValueAsString(payload);
         rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
      } catch (JsonProcessingException e) {
         log.warn("Failed to send VK unlink notification for vkUserId={}", vkUserId, e);
      }
   }

   private void sendTelegramUnlinkNotification(Long telegramUserId) {
      try {
         Map<String, Object> payload = Map.of(
               "telegram_user_id", telegramUserId,
               "text", "Ваш аккаунт Telegram был отвязан от Orkestro.",
               "type", "telegram.unlink");
         String json = objectMapper.writeValueAsString(payload);
         rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
      } catch (JsonProcessingException e) {
         log.warn("Failed to send Telegram unlink notification for telegramUserId={}", telegramUserId, e);
      }
   }
}
