package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.models.user.User;
import io.github.Romariok.orkestro.models.user.UserTelegramLinkToken;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserTelegramLinkTokenRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationListenerTest {

      @Mock
      private UserRepository userRepository;

      @Mock
      private UserTelegramLinkTokenRepository tokenRepository;

      @Mock
      private RabbitTemplate rabbitTemplate;

      @Mock
      private ObjectMapper objectMapper;

      @InjectMocks
      private TelegramNotificationListener listener;

      @BeforeEach
      void setUp() throws Exception {
            // Устанавливаем имя очереди для отправки сообщений боту
            Field field = TelegramNotificationListener.class.getDeclaredField("telegramBotMessageQueueName");
            field.setAccessible(true);
            field.set(listener, "telegram_bot_messages");
      }

      @Test
      void handleTelegramRegistration_nullMessage_doesNotCallRepositoriesOrBroker() {
            listener.handleTelegramRegistration(null);

            verifyNoInteractions(userRepository);
            verifyNoInteractions(tokenRepository);
            verifyNoInteractions(rabbitTemplate);
      }

      @Test
      void handleTelegramRegistration_invalidPayload_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        null, telegramUserId);

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(message);

            verifyNoInteractions(tokenRepository);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_tokenNotFound_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "missing-token";
            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(message);

            verify(tokenRepository).findByToken(token);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_tokenAlreadyUsed_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "used-token";
            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
                        .userId(1L)
                        .token(token)
                        .usedAt(Instant.now())
                        .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(linkToken));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(message);

            verify(tokenRepository).findByToken(token);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_tokenExpired_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "expired-token";
            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
                        .userId(1L)
                        .token(token)
                        .expiresAt(Instant.now().minusSeconds(10))
                        .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(linkToken));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(message);

            verify(tokenRepository).findByToken(token);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_telegramAlreadyLinkedToAnotherUser_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "token";
            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
                        .userId(1L)
                        .token(token)
                        .build();

            User otherUser = User.builder()
                        .id(2L)
                        .username("other")
                        .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(linkToken));
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(otherUser));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(message);

            verify(tokenRepository).findByToken(token);
            verify(userRepository).findByTelegramUserId(telegramUserId);
            verify(userRepository, never()).findById(anyLong());
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_successfullyLinksTelegramAndSendsSuccessMessage() throws Exception {
            Long telegramUserId = 123L;
            String token = "valid-token";
            Long userId = 1L;

            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
                        .userId(userId)
                        .token(token)
                        .build();

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(linkToken));
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

            listener.handleTelegramRegistration(message);

            // Проверяем, что пользователь обновлён и сохранён
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            verify(tokenRepository).save(any(UserTelegramLinkToken.class));

            // Проверяем, что канал уведомлений и telegram_user_id проставлены
            org.junit.jupiter.api.Assertions.assertEquals(NotificationChannelType.TELEGRAM,
                        savedUser.getNotificationChannel());
            org.junit.jupiter.api.Assertions.assertEquals(telegramUserId, savedUser.getTelegramUserId());

            // И что в брокер отправлено сообщение об успешной привязке
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_brokerFailure_doesNotPropagateException() throws Exception {
            Long telegramUserId = 123L;
            String token = "valid-token";
            Long userId = 1L;

            TelegramNotificationListener.TelegramRegistrationMessage message = new TelegramNotificationListener.TelegramRegistrationMessage(
                        token, telegramUserId);

            UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
                        .userId(userId)
                        .token(token)
                        .build();

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(tokenRepository.findByToken(token)).thenReturn(Optional.of(linkToken));
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
            org.mockito.Mockito.doThrow(new RuntimeException("Broker down"))
                        .when(rabbitTemplate).convertAndSend(anyString(), anyString());

            assertDoesNotThrow(() -> listener.handleTelegramRegistration(message));

            // Несмотря на падение брокера, данные пользователя и токена должны быть
            // сохранены
            verify(userRepository).save(any(User.class));
            verify(tokenRepository).save(any(UserTelegramLinkToken.class));
      }
}
