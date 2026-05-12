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
class VkEventNotificationServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VkEventNotificationService vkEventNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vkEventNotificationService, "vkBotMessageQueueName", "vk_notification_queue");
    }

    @Test
    void sendEventCreatedNotification_withVkUserId_sendsToQueueAndReturnsTrue()
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
                .vkUserId(54321L)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"vk_user_id\":54321}");

        boolean result = vkEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "You have an event!");

        assertTrue(result);
        verify(rabbitTemplate).convertAndSend(eq("vk_notification_queue"), anyString());
    }

    @Test
    void sendEventCreatedNotification_withNullVkUserId_returnsFalseWithoutSending() {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .vkUserId(null)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        boolean result = vkEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "You have an event!");

        assertFalse(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void sendEventCommentNotification_withVkUserId_sendsToQueueAndReturnsTrue()
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
                .vkUserId(54321L)
                .preferredLanguage(UserLanguageType.RU)
                .build();

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"vk_user_id\":54321}");

        boolean result = vkEventNotificationService.sendEventCommentNotification(
                event, "Orchestra", user, "New comment on the event");

        assertTrue(result);
        verify(rabbitTemplate).convertAndSend(eq("vk_notification_queue"), anyString());
    }

    @Test
    void sendEventCommentNotification_withNullVkUserId_returnsFalseWithoutSending() {
        Event event = Event.builder()
                .id(1L)
                .organizationId(1L)
                .title("Concert")
                .createdAt(Instant.now())
                .build();

        User user = User.builder()
                .id(10L)
                .username("user")
                .vkUserId(null)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        boolean result = vkEventNotificationService.sendEventCommentNotification(
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
                .vkUserId(54321L)
                .preferredLanguage(UserLanguageType.EN)
                .build();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Error") {});

        boolean result = vkEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "Text");

        assertFalse(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
