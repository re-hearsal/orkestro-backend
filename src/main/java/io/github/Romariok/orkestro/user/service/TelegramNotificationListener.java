package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserTelegramLinkToken;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserTelegramLinkTokenRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationListener {

   private final UserRepository userRepository;
   private final UserTelegramLinkTokenRepository tokenRepository;
   private final RabbitTemplate rabbitTemplate;
   private final ObjectMapper objectMapper;

   @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
   private String telegramBotMessageQueueName;

   public record TelegramRegistrationMessage(
         @JsonProperty("token") String token,
         @JsonProperty("telegram_user_id") Long telegramUserId) {
   }

   @RabbitListener(queues = "${orkestro.telegram.queue-name:telegram_notification_registrations}")
   @Transactional
   public void handleTelegramRegistration(@Payload TelegramRegistrationMessage message) {
      if (message == null || message.token() == null || message.telegramUserId() == null) {
         log.warn("Received empty Telegram registration message: {}", message);
         sendResultToTelegram(message != null ? message.telegramUserId() : null,
               "Не удалось подключить Telegram-уведомления: данные запроса некорректны.");
         return;
      }

      String token = message.token();
      Long telegramUserId = message.telegramUserId();

      Optional<UserTelegramLinkToken> optionalToken = tokenRepository.findByToken(token);

      if (optionalToken.isEmpty()) {
         log.warn("No Telegram link token found for token={}", token);
         sendResultToTelegram(telegramUserId,
               "Не удалось подключить Telegram-уведомления: ссылка недействительна. "
                     + "Сгенерируйте новую ссылку в личном кабинете приложения.");
         return;
      }

      UserTelegramLinkToken linkToken = optionalToken.get();

      if (linkToken.getUsedAt() != null) {
         log.warn("Telegram link token already used, token={}", token);
         sendResultToTelegram(telegramUserId,
               "Ссылка для подключения Telegram-уведомлений уже была использована. "
                     + "Сгенерируйте новую ссылку в личном кабинете приложения.");
         return;
      }

      if (linkToken.getExpiresAt() != null && linkToken.getExpiresAt().isBefore(Instant.now())) {
         log.warn("Telegram link token expired, token={}", token);
         sendResultToTelegram(telegramUserId,
               "Ссылка для подключения Telegram-уведомлений истекла. "
                     + "Пожалуйста, сгенерируйте новую ссылку в личном кабинете приложения.");
         return;
      }

      Long userId = linkToken.getUserId();

      Optional<User> existingByTelegram = userRepository.findByTelegramUserId(telegramUserId);
      if (existingByTelegram.isPresent() && !existingByTelegram.get().getId().equals(userId)) {
         log.warn("Telegram user id={} is already linked to another user id={}", telegramUserId,
               existingByTelegram.get().getId());
         sendResultToTelegram(telegramUserId,
               "Этот Telegram-аккаунт уже привязан к другому пользователю. "
                     + "Если вы считаете это ошибкой, обратитесь к администратору.");
         return;
      }

      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      user.setTelegramUserId(telegramUserId);
      user.setNotificationChannel(NotificationChannelType.TELEGRAM);
      user.setUpdatedAt(Instant.now());
      userRepository.save(user);

      linkToken.setUsedAt(Instant.now());
      tokenRepository.save(linkToken);

      log.info("Enabled TELEGRAM notifications for user id={} (telegram_user_id={})",
            user.getId(), telegramUserId);

      sendResultToTelegram(telegramUserId,
            "✅ Telegram-уведомления успешно подключены к вашему аккаунту.");
   }

   private void sendResultToTelegram(Long telegramUserId, String text) {
      if (telegramUserId == null) {
         log.warn("Cannot send Telegram result message: telegramUserId is null");
         return;
      }

      try {
         Map<String, Object> payload = Map.of(
               "telegram_user_id", telegramUserId,
               "text", text);
         String json = objectMapper.writeValueAsString(payload);
         rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize Telegram result message", e);
      } catch (Exception e) {
         log.error("Failed to send Telegram result message to queue {}", telegramBotMessageQueueName, e);
      }
   }
}
