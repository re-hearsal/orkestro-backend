package io.github.Romariok.orkestro.event.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TelegramEventNotificationServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TelegramEventNotificationService telegramEventNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                telegramEventNotificationService, "telegramBotMessageQueueName", "telegram_bot_messages");
    }

    @Test
    void sendEventCreatedNotification_withTelegramUserId_sendsToQueueAndReturnsTrue()
            throws JsonProcessingException {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .startTime(Instant.now())
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .telegramUserId(12345L)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"telegram_user_id\":12345}");

        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "You have an event!");

        assertTrue(result);
        verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
    }

    @Test
    void sendEventCreatedNotification_withNullTelegramUserId_returnsFalseWithoutSending() {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .telegramUserId(null)
                .preferredLanguage(UserLanguageType.RU)
                .build();

        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "You have an event!");

        assertFalse(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void sendEventCreatedNotification_withRuLanguage_sendsRuLocale() throws JsonProcessingException {
        Event event = Event.builder()
                .id(2L)
                .organizationId(1L)
                .title("Репетиция")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(20L)
                .username("ruUser")
                .telegramUserId(99999L)
                .preferredLanguage(UserLanguageType.RU)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"locale\":\"ru\"}");

        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Оркестр", user, "У вас событие!");

        assertTrue(result);
    }

    @Test
    void sendEventCommentNotification_withTelegramUserId_sendsToQueueAndReturnsTrue()
            throws JsonProcessingException {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .telegramUserId(12345L)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"telegram_user_id\":12345}");

        boolean result = telegramEventNotificationService.sendEventCommentNotification(
                event, "Orchestra", user, "New comment on the event");

        assertTrue(result);
        verify(rabbitTemplate).convertAndSend(eq("telegram_bot_messages"), anyString());
    }

    @Test
    void sendEventCommentNotification_withNullTelegramUserId_returnsFalseWithoutSending() {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .telegramUserId(null)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        boolean result = telegramEventNotificationService.sendEventCommentNotification(
                event, "Orchestra", user, "Comment text");

        assertFalse(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void sendEventCreatedNotification_jsonProcessingException_returnsFalse()
            throws JsonProcessingException {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .telegramUserId(12345L)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization error") {});

        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "Text");

        assertFalse(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
