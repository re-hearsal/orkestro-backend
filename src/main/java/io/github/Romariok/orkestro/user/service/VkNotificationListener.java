package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
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
public class VkNotificationListener {

   private final UserRepository userRepository;
   private final UserVkLinkTokenService tokenService;
   private final UserVkLinkService userVkLinkService;
   private final RabbitTemplate rabbitTemplate;
   private final ObjectMapper objectMapper;

   @Value("${orkestro.vk.bot-message-queue-name:vk_notification_queue}")
   private String vkBotMessageQueueName;

   @JsonIgnoreProperties(ignoreUnknown = true)
   public record VkRegistrationMessage(
         @JsonProperty("request_id") String requestId,
         @JsonProperty("token") String token,
         @JsonProperty("vk_user_id") Long vkUserId) {
   }

   @RabbitListener(queues = "${orkestro.vk.queue-name:vk_notification_registrations}")
   @Transactional
   public void handleVkRegistration(Message amqpMessage) {
      if (amqpMessage == null || amqpMessage.getBody() == null) {
         log.warn("Received null/empty AMQP message for VK registration");
         return;
      }

      VkRegistrationMessage message;
      try {
         message = objectMapper.readValue(amqpMessage.getBody(), VkRegistrationMessage.class);
      } catch (Exception ex) {
         log.error("Failed to parse VK registration message from RabbitMQ", ex);
         return;
      }

      String requestId = message.requestId();
      Long vkUserId = message.vkUserId();
      String token = message.token();

      if (token == null || vkUserId == null) {
         log.warn("Received invalid VK registration message: {}", message);
         sendResultToVk(vkUserId, requestId, "Некорректный запрос. Попробуйте получить новый токен.");
         return;
      }

      Long userId = null;
      try {
         UserVkLinkTokenService.ParsedVkLinkToken parsedToken;
         try {
            parsedToken = tokenService.parseToken(token);
         } catch (Exception ex) {
            log.warn("Invalid VK link token: {}", ex.getMessage());
            sendResultToVk(vkUserId, requestId, "Токен недействителен или истёк. Получите новый токен в приложении.");
            return;
         }

         userId = parsedToken.userId();
         final Long resolvedUserId = userId;

         Optional<User> existingByVk = userRepository.findByVkUserId(vkUserId);
         if (existingByVk.isPresent() && !existingByVk.get().getId().equals(resolvedUserId)) {
            log.warn("VK user id={} is already linked to another user id={}", vkUserId,
                  existingByVk.get().getId());
            sendResultToVk(vkUserId, requestId,
                  "Этот аккаунт ВКонтакте уже привязан к другому пользователю.");
            return;
         }

         userVkLinkService.linkVk(token, vkUserId);

         log.info("Enabled VK notifications for user id={} (vk_user_id={})", resolvedUserId, vkUserId);
         sendResultToVk(vkUserId, requestId, "Аккаунт ВКонтакте успешно привязан к Orkestro!");
      } catch (Exception ex) {
         log.error("Failed to handle VK registration request", ex);
         sendResultToVk(vkUserId, requestId, "Произошла ошибка сервера. Попробуйте позже.");
      }
   }

   private void sendResultToVk(Long vkUserId, String requestId, String text) {
      if (vkUserId == null) {
         log.warn("Cannot send VK result message: vkUserId is null");
         return;
      }

      try {
         Map<String, Object> payload = new HashMap<>();
         payload.put("vk_user_id", vkUserId);
         payload.put("text", text);
         if (requestId != null) {
            payload.put("request_id", requestId);
         }
         String json = objectMapper.writeValueAsString(payload);
         rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
      } catch (JsonProcessingException e) {
         log.error("Failed to serialize VK result message", e);
      } catch (Exception e) {
         log.error("Failed to send VK result message to queue {}", vkBotMessageQueueName, e);
      }
   }
}
