package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.service.UserVkLinkService;
import io.github.Romariok.orkestro.user.service.UserVkLinkTokenService;
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
class UserVkLinkServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserVkLinkTokenService tokenService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserVkLinkService userVkLinkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userVkLinkService, "vkBotMessageQueueName", "vk_notification_queue");
        ReflectionTestUtils.setField(userVkLinkService, "telegramBotMessageQueueName", "telegram_bot_messages");
    }

    // ── generateLinkToken ──────────────────────────────────────────────────

    @Test
    void generateLinkToken_userNotFound_throwsEntityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userVkLinkService.generateLinkToken(1L));
        verify(tokenService, never()).createToken(any());
    }

    @Test
    void generateLinkToken_validUser_createsToken() {
        User user = User.builder().id(1L).username("user").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tokenService.createToken(1L)).thenReturn("vk-token");

        String result = userVkLinkService.generateLinkToken(1L);

        assertEquals("vk-token", result);
        verify(tokenService).createToken(1L);
    }

    // ── linkVk ────────────────────────────────────────────────────────────

    @Test
    void linkVk_invalidToken_throwsIllegalArgument() {
        when(tokenService.parseToken("bad-token")).thenThrow(new IllegalArgumentException("Invalid VK link token"));

        assertThrows(IllegalArgumentException.class, () -> userVkLinkService.linkVk("bad-token", 100L));
    }

    @Test
    void linkVk_noExistingMessenger_linksVkAndSetsChannel() {
        Long userId = 1L;
        Long vkUserId = 100L;
        User user = User.builder()
                .id(userId)
                .username("user")
                .telegramUserId(null)
                .vkUserId(null)
                .notificationChannel(NotificationChannelType.EMAIL)
                .build();

        when(tokenService.parseToken("valid-token")).thenReturn(new UserVkLinkTokenService.ParsedVkLinkToken(userId, 60L));
        when(userRepository.findByVkUserId(vkUserId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userVkLinkService.linkVk("valid-token", vkUserId);

        assertEquals(vkUserId, user.getVkUserId());
        assertNull(user.getTelegramUserId());
        assertEquals(NotificationChannelType.VK, user.getNotificationChannel());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void linkVk_withExistingTelegram_sendsUnlinkAndClearsTelegram() throws Exception {
        Long userId = 1L;
        Long vkUserId = 100L;
        Long existingTelegramId = 999L;

        User user = User.builder()
                .id(userId)
                .username("user")
                .telegramUserId(existingTelegramId)
                .vkUserId(null)
                .notificationChannel(NotificationChannelType.TELEGRAM)
                .build();

        when(tokenService.parseToken("valid-token")).thenReturn(new UserVkLinkTokenService.ParsedVkLinkToken(userId, 60L));
        when(userRepository.findByVkUserId(vkUserId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userVkLinkService.linkVk("valid-token", vkUserId);

        assertNull(user.getTelegramUserId());
        assertEquals(vkUserId, user.getVkUserId());
        assertEquals(NotificationChannelType.VK, user.getNotificationChannel());
        verify(rabbitTemplate).convertAndSend(anyString(), eq("{}"));
    }

    @Test
    void linkVk_vkUserIdAlreadyLinkedToOtherUser_throwsIllegalArgument() {
        Long userId = 1L;
        Long vkUserId = 100L;
        User otherUser = User.builder().id(2L).build();

        when(tokenService.parseToken("valid-token")).thenReturn(new UserVkLinkTokenService.ParsedVkLinkToken(userId, 60L));
        when(userRepository.findByVkUserId(vkUserId)).thenReturn(Optional.of(otherUser));

        assertThrows(IllegalArgumentException.class, () -> userVkLinkService.linkVk("valid-token", vkUserId));
        verify(userRepository, never()).save(any());
    }

    // ── unlinkVk ──────────────────────────────────────────────────────────

    @Test
    void unlinkVk_userNotFound_throwsEntityNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userVkLinkService.unlinkVk(1L));
    }

    @Test
    void unlinkVk_withExistingVk_sendsUnlinkNotificationAndClearsVkId() throws Exception {
        Long userId = 1L;
        Long vkUserId = 100L;

        User user = User.builder()
                .id(userId)
                .username("user")
                .vkUserId(vkUserId)
                .notificationChannel(NotificationChannelType.VK)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(userRepository.save(any(User.class))).thenReturn(user);

        userVkLinkService.unlinkVk(userId);

        assertNull(user.getVkUserId());
        assertEquals(NotificationChannelType.EMAIL, user.getNotificationChannel());
        verify(rabbitTemplate).convertAndSend(anyString(), eq("{}"));
    }

    @Test
    void unlinkVk_withNoVkLinked_setsChannelToEmailWithoutNotification() {
        Long userId = 1L;

        User user = User.builder()
                .id(userId)
                .username("user")
                .vkUserId(null)
                .notificationChannel(NotificationChannelType.EMAIL)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userVkLinkService.unlinkVk(userId);

        assertNull(user.getVkUserId());
        assertEquals(NotificationChannelType.EMAIL, user.getNotificationChannel());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
