package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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

   @Value("${orkestro.telegram.contract.type.telegram-link:telegram.link}")
   private String telegramLinkType;

   private final String statusOk = "OK";

   private final String statusError = "ERROR";

   @Value("${orkestro.telegram.messages.link.invalid-request:Не удалось подключить Telegram-уведомления: данные запроса некорректны.}")
   private String msgLinkInvalidRequest;

   @Value("${orkestro.telegram.messages.link.token-not-found:Не удалось подключить Telegram-уведомления: ссылка недействительна. Сгенерируйте новую ссылку в личном кабинете приложения.}")
   private String msgLinkTokenNotFound;

   @Value("${orkestro.telegram.messages.link.token-already-used:Ссылка для подключения Telegram-уведомлений уже была использована. Сгенерируйте новую ссылку в личном кабинете приложения.}")
   private String msgLinkTokenAlreadyUsed;

   @Value("${orkestro.telegram.messages.link.token-expired:Ссылка для подключения Telegram-уведомлений истекла. Пожалуйста, сгенерируйте новую ссылку в личном кабинете приложения.}")
   private String msgLinkTokenExpired;

   @Value("${orkestro.telegram.messages.link.telegram-already-linked:Этот Telegram-аккаунт уже привязан к другому пользователю. Если вы считаете это ошибкой, обратитесь к администратору.}")
   private String msgLinkTelegramAlreadyLinked;

   @Value("${orkestro.telegram.messages.link.success:✅ Telegram-уведомления успешно подключены к вашему аккаунту.}")
   private String msgLinkSuccess;

   @Value("${orkestro.telegram.messages.link.server-error:Не удалось подключить Telegram-уведомления из-за ошибки сервера. Попробуйте позже.}")
   private String msgLinkServerError;

   @JsonIgnoreProperties(ignoreUnknown = true)
   public record TelegramRegistrationMessage(
         @JsonProperty("request_id") String requestId,
         @JsonProperty("type") String type,
         @JsonProperty("token") String token,
         @JsonProperty("telegram_user_id") Long telegramUserId) {
   }

   @RabbitListener(queues = "${orkestro.telegram.queue-name:telegram_notification_registrations}")
   @Transactional
   public void handleTelegramRegistration(Message amqpMessage) {
      if (amqpMessage == null || amqpMessage.getBody() == null) {
         log.warn("Received null/empty AMQP message for Telegram registration");
         return;
      }

      TelegramRegistrationMessage message;
      try {
         message = objectMapper.readValue(amqpMessage.getBody(), TelegramRegistrationMessage.class);
      } catch (Exception ex) {
         log.error("Failed to parse Telegram registration message from RabbitMQ", ex);
         return;
      }

      String requestId = message.requestId();
      String defaultType = (telegramLinkType == null || telegramLinkType.isBlank()) ? "telegram.link" : telegramLinkType;
      String type = message.type() == null || message.type().isBlank() ? defaultType : message.type();
      Long telegramUserId = message.telegramUserId();
      String token = message.token();

      if (token == null || telegramUserId == null) {
         log.warn("Received invalid Telegram registration message: {}", message);
         sendResultToTelegram(
               telegramUserId,
               requestId,
               type,
               (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
               msgLinkInvalidRequest);
         return;
      }

      try {
         Optional<UserTelegramLinkToken> optionalToken = tokenRepository.findByToken(token);

         if (optionalToken.isEmpty()) {
            log.warn("No Telegram link token found for token={}", token);
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  type,
                  (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                  msgLinkTokenNotFound);
            return;
         }

         UserTelegramLinkToken linkToken = optionalToken.get();

         if (linkToken.getUsedAt() != null) {
            log.warn("Telegram link token already used, token={}", token);
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  type,
                  (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                  msgLinkTokenAlreadyUsed);
            return;
         }

         if (linkToken.getExpiresAt() != null && linkToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Telegram link token expired, token={}", token);
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  type,
                  (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                  msgLinkTokenExpired);
            return;
         }

         Long userId = linkToken.getUserId();

         Optional<User> existingByTelegram = userRepository.findByTelegramUserId(telegramUserId);
         if (existingByTelegram.isPresent() && !existingByTelegram.get().getId().equals(userId)) {
            log.warn("Telegram user id={} is already linked to another user id={}", telegramUserId,
                  existingByTelegram.get().getId());
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  type,
                  (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
                  msgLinkTelegramAlreadyLinked);
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

         sendResultToTelegram(
               telegramUserId,
               requestId,
               type,
               (statusOk == null || statusOk.isBlank()) ? "OK" : statusOk,
               msgLinkSuccess);
      } catch (Exception ex) {
         log.error("Failed to handle Telegram registration request", ex);
         sendResultToTelegram(
               telegramUserId,
               requestId,
               type,
               (statusError == null || statusError.isBlank()) ? "ERROR" : statusError,
               msgLinkServerError);
      }
   }

   private void sendResultToTelegram(
         Long telegramUserId,
         String requestId,
         String type,
         String status,
         String text) {
      if (telegramUserId == null) {
         log.warn("Cannot send Telegram result message: telegramUserId is null");
         return;
      }

      try {
         java.util.Map<String, Object> payload = new java.util.HashMap<>();
         payload.put("telegram_user_id", telegramUserId);
         payload.put("text", text);
         if (requestId != null) {
            payload.put("request_id", requestId);
         }
         if (type != null) {
            payload.put("type", type);
         }
         if (status != null) {
            payload.put("status", status);
         }
         String json = objectMapper.writeValueAsString(payload);
         rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize Telegram result message", e);
      } catch (Exception e) {
         log.error("Failed to send Telegram result message to queue {}", telegramBotMessageQueueName, e);
      }
   }
}
