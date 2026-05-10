package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramNotificationListener {

   private final UserRepository userRepository;
   private final UserTelegramLinkTokenService tokenService;
   private final UserTelegramLinkService userTelegramLinkService;
   private final RabbitTemplate rabbitTemplate;
   private final ObjectMapper objectMapper;
   private final MessageSource messageSource;

   @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
   private String telegramBotMessageQueueName;

   @JsonIgnoreProperties(ignoreUnknown = true)
   public record TelegramRegistrationMessage(
         @JsonProperty("request_id") String requestId,
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
      Long telegramUserId = message.telegramUserId();
      String token = message.token();

      if (token == null || telegramUserId == null) {
         log.warn("Received invalid Telegram registration message: {}", message);
         sendResultToTelegram(
               telegramUserId,
               requestId,
               Locale.forLanguageTag("ru"),
               getMessage("notification.telegram.link.invalid-request", Locale.forLanguageTag("ru")));
         return;
      }

      Long userId = null;
      try {
         UserTelegramLinkTokenService.ParsedTelegramLinkToken parsedToken;
         try {
            parsedToken = tokenService.parseToken(token);
         } catch (Exception ex) {
            log.warn("Invalid Telegram link token: {}", ex.getMessage());
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  Locale.forLanguageTag("ru"),
                  getMessage("notification.telegram.link.token-not-found", Locale.forLanguageTag("ru")));
            return;
         }

         userId = parsedToken.userId();
         final Long resolvedUserId = userId;

         Optional<User> existingByTelegram = userRepository.findByTelegramUserId(telegramUserId);
         if (existingByTelegram.isPresent() && !existingByTelegram.get().getId().equals(resolvedUserId)) {
            log.warn("Telegram user id={} is already linked to another user id={}", telegramUserId,
                  existingByTelegram.get().getId());
            Locale locale = resolveLocaleByUserId(resolvedUserId);
            sendResultToTelegram(
                  telegramUserId,
                  requestId,
                  locale,
                  getMessage("notification.telegram.link.telegram-already-linked", locale));
            return;
         }

         userTelegramLinkService.linkTelegram(resolvedUserId, telegramUserId);

         User user = userRepository.findById(resolvedUserId)
               .orElseThrow(() -> new EntityNotFoundException("User not found: " + resolvedUserId));

         log.info("Enabled TELEGRAM notifications for user id={} (telegram_user_id={})", userId, telegramUserId);

         Locale locale = resolveLocale(user);
         sendResultToTelegram(
               telegramUserId,
               requestId,
               locale,
               getMessage("notification.telegram.link.success", locale));
      } catch (Exception ex) {
         log.error("Failed to handle Telegram registration request", ex);
         Locale locale = resolveLocaleByUserId(userId);
         sendResultToTelegram(
               telegramUserId,
               requestId,
               locale,
               getMessage("notification.telegram.link.server-error", locale));
      }
   }

   private void sendResultToTelegram(
         Long telegramUserId,
         String requestId,
         Locale locale,
         String text) {
      if (telegramUserId == null) {
         log.warn("Cannot send Telegram result message: telegramUserId is null");
         return;
      }

      try {
         Map<String, Object> payload = new HashMap<>();
         payload.put("telegram_user_id", telegramUserId);
         payload.put("text", text);
         payload.put("locale", locale.getLanguage().equals("en") ? "en" : "ru");
         if (requestId != null) {
            payload.put("request_id", requestId);
         }
         rabbitTemplate.convertAndSend(telegramBotMessageQueueName, objectMapper.writeValueAsBytes(payload));
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize Telegram result message", e);
      } catch (Exception e) {
         log.error("Failed to send Telegram result message to queue {}", telegramBotMessageQueueName, e);
      }
   }

   private Locale resolveLocale(User user) {
      UserLanguageType language = user.getPreferredLanguage() == null ? UserLanguageType.RU : user.getPreferredLanguage();
      return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
   }

   private Locale resolveLocaleByUserId(Long userId) {
      if (userId == null) {
         return Locale.forLanguageTag("ru");
      }
      UserLanguageType language = userRepository.findById(userId)
            .map(User::getPreferredLanguage)
            .orElse(UserLanguageType.RU);
      return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
   }

   private String getMessage(String key, Locale locale) {
      return messageSource.getMessage(key, null, locale);
   }
}
