package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkTokenService;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserTelegramLinkServiceTest {

      @Mock
      private UserRepository userRepository;

      @Mock
      private UserTelegramLinkTokenService tokenService;

      @Mock
      private SecurityUtils securityUtils;

      @Mock
      private RabbitTemplate rabbitTemplate;

      @Mock
      private ObjectMapper objectMapper;

      @InjectMocks
      private UserTelegramLinkService userTelegramLinkService;

      @BeforeEach
      void setUp() {
            ReflectionTestUtils.setField(userTelegramLinkService, "vkBotMessageQueueName", "vk_notification_queue");
            ReflectionTestUtils.setField(userTelegramLinkService, "telegramBotMessageQueueName", "telegram_bot_messages");
      }

      @Test
      void createLinkTokenForUser_userNotFound_throwsEntityNotFound() {
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                        () -> userTelegramLinkService.createLinkTokenForUser(userId));

            verify(tokenService, never()).createToken(any());
      }

      @Test
      void createLinkTokenForUser_validUser_createsToken() {
            Long userId = 1L;
            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenService.createToken(userId)).thenReturn("generated-token");

            String generatedToken = userTelegramLinkService.createLinkTokenForUser(userId);

            assertEquals("generated-token", generatedToken);
            verify(tokenService).createToken(userId);
      }

      @Test
      void createLinkTokenForCurrentUser_usesCurrentUserIdFromSecurityUtils() {
            Long currentUserId = 5L;
            User user = User.builder()
                        .id(currentUserId)
                        .username("current")
                        .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
            when(tokenService.createToken(currentUserId)).thenReturn("token");

            String token = userTelegramLinkService.createLinkTokenForCurrentUser();

            assertNotNull(token);
            verify(userRepository).findById(currentUserId);
            verify(tokenService).createToken(currentUserId);
      }

      @Test
      void linkTelegram_userNotFound_throwsEntityNotFound() {
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                        () -> userTelegramLinkService.linkTelegram(userId, 12345L));

            verify(userRepository, never()).save(any());
      }

      @Test
      void linkTelegram_withExistingVk_replacesVkWithTelegram() throws Exception {
            Long userId = 1L;
            Long existingVkUserId = 777L;
            Long newTelegramUserId = 12345L;

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .vkUserId(existingVkUserId)
                        .telegramUserId(null)
                        .notificationChannel(NotificationChannelType.VK)
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(userRepository.save(any(User.class))).thenReturn(user);

            userTelegramLinkService.linkTelegram(userId, newTelegramUserId);

            assertNull(user.getVkUserId());
            assertEquals(newTelegramUserId, user.getTelegramUserId());
            assertEquals(NotificationChannelType.TELEGRAM, user.getNotificationChannel());
            verify(rabbitTemplate).convertAndSend(anyString(), eq("{}"));
            verify(userRepository).save(user);
      }

      @Test
      void linkTelegram_noExistingMessenger_linksTelegramDirectly() {
            Long userId = 1L;
            Long newTelegramUserId = 12345L;

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .vkUserId(null)
                        .telegramUserId(null)
                        .notificationChannel(NotificationChannelType.EMAIL)
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userTelegramLinkService.linkTelegram(userId, newTelegramUserId);

            assertEquals(newTelegramUserId, user.getTelegramUserId());
            assertEquals(NotificationChannelType.TELEGRAM, user.getNotificationChannel());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
      }

      @Test
      void unlinkTelegram_withExistingTelegram_sendsNotificationAndClears() throws Exception {
            Long userId = 1L;
            Long telegramUserId = 12345L;

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .telegramUserId(telegramUserId)
                        .notificationChannel(NotificationChannelType.TELEGRAM)
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(userRepository.save(any(User.class))).thenReturn(user);

            userTelegramLinkService.unlinkTelegram(userId);

            assertNull(user.getTelegramUserId());
            assertEquals(NotificationChannelType.EMAIL, user.getNotificationChannel());
            verify(rabbitTemplate).convertAndSend(anyString(), eq("{}"));
            verify(userRepository).save(user);
      }

      @Test
      void unlinkTelegram_withNoTelegram_setsEmailChannelWithoutNotification() {
            Long userId = 1L;

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .telegramUserId(null)
                        .notificationChannel(NotificationChannelType.EMAIL)
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userTelegramLinkService.unlinkTelegram(userId);

            assertEquals(NotificationChannelType.EMAIL, user.getNotificationChannel());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
      }

      @Test
      void unlinkTelegram_userNotFound_throwsEntityNotFoundException() {
            Long userId = 99L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                        () -> userTelegramLinkService.unlinkTelegram(userId));

            verify(userRepository, never()).save(any());
      }
}
