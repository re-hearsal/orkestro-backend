package io.github.Romariok.orkestro.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVkLinkService {

    private final UserRepository userRepository;
    private final UserVkLinkTokenService tokenService;
    private final SecurityUtils securityUtils;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${orkestro.vk.bot-message-queue-name:vk_notification_queue}")
    private String vkBotMessageQueueName;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueueName;

    @Transactional
    public String generateLinkTokenForCurrentUser() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return generateLinkToken(currentUserId);
    }

    @Transactional
    public String generateLinkToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        String token = tokenService.createToken(user.getId());
        log.info("Created VK link token for user id={}", userId);
        return token;
    }

    @Transactional
    public void linkVk(String token, Long vkUserId) {
        UserVkLinkTokenService.ParsedVkLinkToken parsed = tokenService.parseToken(token);
        Long userId = parsed.userId();

        Optional<User> existingByVk = userRepository.findByVkUserId(vkUserId);
        if (existingByVk.isPresent() && !existingByVk.get().getId().equals(userId)) {
            throw new IllegalArgumentException("VK user id " + vkUserId + " is already linked to another account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (user.getTelegramUserId() != null) {
            sendTelegramUnlinkNotification(user.getTelegramUserId());
            user.setTelegramUserId(null);
        }

        user.setVkUserId(vkUserId);
        user.setNotificationChannel(NotificationChannelType.VK);
        userRepository.save(user);

        log.info("Linked VK user {} to user id={}", vkUserId, userId);
    }

    @Transactional
    public void unlinkVk(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        if (user.getVkUserId() != null) {
            sendVkUnlinkNotification(user.getVkUserId());
            user.setVkUserId(null);
        }
        user.setNotificationChannel(NotificationChannelType.EMAIL);
        userRepository.save(user);

        log.info("Unlinked VK from user id={}", userId);
    }

    private void sendTelegramUnlinkNotification(Long telegramUserId) {
        try {
            Map<String, Object> payload = Map.of(
                    "telegram_user_id", telegramUserId,
                    "text", "Ваш аккаунт Telegram был отвязан, так как вы подключили ВКонтакте.",
                    "type", "telegram.unlink");
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(telegramBotMessageQueueName, json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to send Telegram unlink notification for telegramUserId={}", telegramUserId, e);
        }
    }

    private void sendVkUnlinkNotification(Long vkUserId) {
        try {
            Map<String, Object> payload = Map.of(
                    "vk_user_id", vkUserId,
                    "text", "Ваш аккаунт ВКонтакте был отвязан от Orkestro.",
                    "type", "vk.unlink");
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(vkBotMessageQueueName, json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to send VK unlink notification for vkUserId={}", vkUserId, e);
        }
    }
}
