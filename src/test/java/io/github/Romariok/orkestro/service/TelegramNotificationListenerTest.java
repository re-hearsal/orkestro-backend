package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.service.TelegramNotificationListener;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkTokenService;

import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationListenerTest {

      @Mock
      private UserRepository userRepository;

      @Mock
      private UserTelegramLinkTokenService tokenService;

      @Mock
      private RabbitTemplate rabbitTemplate;

      @Mock
      private ObjectMapper objectMapper;

      @Mock
      private MessageSource messageSource;

      @InjectMocks
      private TelegramNotificationListener listener;

      @BeforeEach
      void setUp() throws Exception {
            // Устанавливаем имя очереди для отправки сообщений боту
            Field field = TelegramNotificationListener.class.getDeclaredField("telegramBotMessageQueueName");
            field.setAccessible(true);
            field.set(listener, "telegram_bot_messages");
      }

      private Message amqpMessageFor(TelegramNotificationListener.TelegramRegistrationMessage msg) throws Exception {
            MessageProperties props = new MessageProperties();
            props.setContentType("application/json");
            return new Message("{\"x\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8), props);
      }

      private TelegramNotificationListener.TelegramRegistrationMessage message(
                  String requestId,
                  String type,
                  String token,
                  Long telegramUserId) {
            return org.mockito.Mockito.mock(TelegramNotificationListener.TelegramRegistrationMessage.class,
                        invocation -> switch (invocation.getMethod().getName()) {
                              case "requestId", "getRequestId" -> requestId;
                              case "type", "getType" -> type;
                              case "token", "getToken" -> token;
                              case "telegramUserId", "getTelegramUserId" -> telegramUserId;
                              default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
                        });
      }

      @Test
      void handleTelegramRegistration_nullMessage_doesNotCallRepositoriesOrBroker() {
            listener.handleTelegramRegistration(null);

            verifyNoInteractions(userRepository);
            verifyNoInteractions(tokenService);
            verifyNoInteractions(rabbitTemplate);
      }

      @Test
      void handleTelegramRegistration_invalidPayload_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            TelegramNotificationListener.TelegramRegistrationMessage parsed = message("req", "telegram.link", null,
                        telegramUserId);
            Message amqp = amqpMessageFor(parsed);

            when(objectMapper.readValue(any(byte[].class), eq(TelegramNotificationListener.TelegramRegistrationMessage.class)))
                        .thenReturn(parsed);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(amqp);

            verifyNoInteractions(tokenService);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_invalidToken_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "missing-token";
            TelegramNotificationListener.TelegramRegistrationMessage parsed = message("req", "telegram.link", token,
                        telegramUserId);
            Message amqp = amqpMessageFor(parsed);

            when(tokenService.parseToken(token)).thenThrow(new IllegalArgumentException("bad token"));
            when(objectMapper.readValue(any(byte[].class), eq(TelegramNotificationListener.TelegramRegistrationMessage.class)))
                        .thenReturn(parsed);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(amqp);

            verify(tokenService).parseToken(token);
            verifyNoInteractions(userRepository);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_telegramAlreadyLinkedToAnotherUser_sendsErrorToTelegram() throws Exception {
            Long telegramUserId = 123L;
            String token = "token";
            Long userId = 1L;
            TelegramNotificationListener.TelegramRegistrationMessage parsed = message("req", "telegram.link", token,
                        telegramUserId);
            Message amqp = amqpMessageFor(parsed);

            UserTelegramLinkTokenService.ParsedTelegramLinkToken parsedToken = new UserTelegramLinkTokenService.ParsedTelegramLinkToken(
                        userId, 60L);

            User otherUser = User.builder()
                        .id(2L)
                        .username("other")
                        .build();

            when(tokenService.parseToken(token)).thenReturn(parsedToken);
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.of(otherUser));
            when(objectMapper.readValue(any(byte[].class), eq(TelegramNotificationListener.TelegramRegistrationMessage.class)))
                        .thenReturn(parsed);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":false}");

            listener.handleTelegramRegistration(amqp);

            verify(tokenService).parseToken(token);
            verify(userRepository).findByTelegramUserId(telegramUserId);
            verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
      }

      @Test
      void handleTelegramRegistration_successfullyLinksTelegramAndSendsSuccessMessage() throws Exception {
            Long telegramUserId = 123L;
            String token = "valid-token";
            Long userId = 1L;

            TelegramNotificationListener.TelegramRegistrationMessage parsed = message("req", "telegram.link", token,
                        telegramUserId);
            Message amqp = amqpMessageFor(parsed);

            UserTelegramLinkTokenService.ParsedTelegramLinkToken parsedToken = new UserTelegramLinkTokenService.ParsedTelegramLinkToken(
                        userId, 60L);

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(tokenService.parseToken(token)).thenReturn(parsedToken);
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.readValue(any(byte[].class), eq(TelegramNotificationListener.TelegramRegistrationMessage.class)))
                        .thenReturn(parsed);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");

            listener.handleTelegramRegistration(amqp);

            // Проверяем, что пользователь обновлён и сохранён
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

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

            TelegramNotificationListener.TelegramRegistrationMessage parsed = message("req", "telegram.link", token,
                        telegramUserId);
            Message amqp = amqpMessageFor(parsed);

            UserTelegramLinkTokenService.ParsedTelegramLinkToken parsedToken = new UserTelegramLinkTokenService.ParsedTelegramLinkToken(
                        userId, 60L);

            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(tokenService.parseToken(token)).thenReturn(parsedToken);
            when(userRepository.findByTelegramUserId(telegramUserId)).thenReturn(Optional.empty());
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(objectMapper.readValue(any(byte[].class), eq(TelegramNotificationListener.TelegramRegistrationMessage.class)))
                        .thenReturn(parsed);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"ok\":true}");
            org.mockito.Mockito.doThrow(new RuntimeException("Broker down"))
                        .when(rabbitTemplate).convertAndSend(anyString(), anyString());

            assertDoesNotThrow(() -> listener.handleTelegramRegistration(amqp));

            // Несмотря на падение брокера, данные пользователя должны быть сохранены
            verify(userRepository).save(any(User.class));
      }
}
