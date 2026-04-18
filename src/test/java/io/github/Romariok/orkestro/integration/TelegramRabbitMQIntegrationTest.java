package io.github.Romariok.orkestro.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.event.service.TelegramEventNotificationService;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TelegramRabbitMQIntegrationTest extends AbstractRabbitMQIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationUserRepository organizationUserRepository;

    @MockitoSpyBean
    private TelegramEventNotificationService telegramEventNotificationService;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private UserTelegramLinkTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${orkestro.telegram.bot-message-queue-name:telegram_bot_messages}")
    private String telegramBotMessageQueue;

    @Value("${orkestro.telegram.queue-name:telegram_notification_registrations}")
    private String telegramRegistrationQueue;

    @BeforeEach
    void cleanUp() {
        organizationUserRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    /**
     * Scenario 1 (task 5.3): TelegramEventNotificationService publishes message to queue.
     */
    @Test
    @Order(1)
    void sendEventCreatedNotification_publishesMessageToTelegramBotQueue() {
        // given
        User user = User.builder()
                .username("tg_notify_user")
                .name("Telegram Notify User")
                .email("tg_notify@example.com")
                .password("pass")
                .notificationChannel(NotificationChannelType.TELEGRAM)
                .preferredLanguage(UserLanguageType.EN)
                .telegramUserId(123456L)
                .build();
        Event event = buildTransientEvent(1L, 1L, "Spring Concert");

        // when
        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Test Orchestra", user, "You are invited to Spring Concert");

        // then
        assertThat(result).isTrue();
        Object received = rabbitTemplate.receiveAndConvert(telegramBotMessageQueue, 5000);
        assertThat(received).isNotNull().isInstanceOf(String.class);
        assertThat((String) received).contains("telegram_user_id").contains("123456");
    }

    /**
     * Scenario 2 (task 5.4): Registration message consumed by TelegramNotificationListener,
     * Telegram account linked to user.
     */
    @Test
    @Order(2)
    void registrationMessage_processedByListener_linksUserTelegram() throws Exception {
        // given
        User user = userRepository.save(User.builder()
                .username("link_user")
                .name("Link User")
                .email("link@example.com")
                .password("pass")
                .notificationChannel(NotificationChannelType.EMAIL)
                .preferredLanguage(UserLanguageType.EN)
                .build());
        String token = tokenService.createToken(user.getId());
        long telegramUserId = 777888L;

        String message = objectMapper.writeValueAsString(Map.of(
                "request_id", "req-test-1",
                "token", token,
                "telegram_user_id", telegramUserId));

        // when
        rabbitTemplate.convertAndSend(telegramRegistrationQueue, message);

        // then - listener should process and link the user
        await().atMost(10, SECONDS).untilAsserted(() -> {
            User updated = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updated.getTelegramUserId()).isEqualTo(telegramUserId);
            assertThat(updated.getNotificationChannel()).isEqualTo(NotificationChannelType.TELEGRAM);
        });
    }

    /**
     * Scenario 3 (task 5.5): Message with null telegram_user_id is handled gracefully
     * — listener does not throw, application context remains healthy.
     */
    @Test
    @Order(3)
    void registrationMessage_withNullTelegramUserId_listenerHandlesGracefully() {
        // given - registration message with null telegram_user_id
        String message = "{\"request_id\":\"req-null\",\"token\":\"sometoken\",\"telegram_user_id\":null}";

        // when
        rabbitTemplate.convertAndSend(telegramRegistrationQueue, message);

        // then - application context is still alive; verify a subsequent notification works normally
        await().atMost(5, SECONDS).untilAsserted(() -> {
            User user = User.builder()
                    .username("healthy_check_user")
                    .name("Health Check")
                    .email("health@example.com")
                    .password("pass")
                    .notificationChannel(NotificationChannelType.TELEGRAM)
                    .preferredLanguage(UserLanguageType.EN)
                    .telegramUserId(999L)
                    .build();
            boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                    buildTransientEvent(1L, 1L, "Health Check"), "Org", user, "ping");
            assertThat(result).isTrue();
        });

        // drain the queue to avoid interference with subsequent tests
        rabbitTemplate.receiveAndConvert(telegramBotMessageQueue, 1000);
    }

    /**
     * Scenario 4 (task 5.7): Five registration messages are all processed by the listener.
     */
    @Test
    @Order(4)
    void registrationMessages_fiveMessages_allProcessedByListener() throws Exception {
        // given
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(userRepository.save(User.builder()
                    .username("batch_user_" + i)
                    .name("Batch User " + i)
                    .email("batch" + i + "@example.com")
                    .password("pass")
                    .notificationChannel(NotificationChannelType.EMAIL)
                    .preferredLanguage(UserLanguageType.EN)
                    .build()));
        }

        // when - publish 5 registration messages
        for (int i = 0; i < 5; i++) {
            long telegramId = 50000L + i;
            String msg = objectMapper.writeValueAsString(Map.of(
                    "request_id", "batch-req-" + i,
                    "token", tokenService.createToken(users.get(i).getId()),
                    "telegram_user_id", telegramId));
            rabbitTemplate.convertAndSend(telegramRegistrationQueue, msg);
        }

        // then - all 5 users should be linked to their Telegram accounts
        await().atMost(15, SECONDS).untilAsserted(() -> {
            long linked = users.stream()
                    .map(u -> userRepository.findById(u.getId()).orElseThrow())
                    .filter(u -> u.getTelegramUserId() != null)
                    .count();
            assertThat(linked).isEqualTo(5);
        });
    }

    /**
     * Scenario 5 (task 5.8): Notification for a VK-channel user does NOT invoke
     * TelegramEventNotificationService.
     */
    @Test
    @Order(5)
    void sendEventCreatedNotifications_vkChannelUser_telegramServiceNeverCalled() {
        // given
        Organization org = organizationRepository.save(
                Organization.builder().name("VK Test Org").location("City").build());
        User vkUser = userRepository.save(User.builder()
                .username("vk_notify_user")
                .name("VK Notify User")
                .email("vk_notify@example.com")
                .password("pass")
                .notificationChannel(NotificationChannelType.VK)
                .preferredLanguage(UserLanguageType.EN)
                .vkUserId(987654L)
                .build());
        Event event = buildTransientEvent(org.getId(), 1L, "VK Concert");

        // when
        eventNotificationService.sendEventCreatedNotifications(event, List.of(vkUser.getId()));

        // then - TelegramEventNotificationService must not be called for VK user
        verify(telegramEventNotificationService, never())
                .sendEventCreatedNotification(any(), anyString(), any(), anyString());
    }

    /**
     * Scenario 6 (task 5.6): When RabbitMQ broker is unavailable,
     * sendEventCreatedNotification returns false (no NPE, no uncaught exception).
     * This test MUST run last as it stops the shared RabbitMQ container.
     */
    @Test
    @Order(6)
    void sendEventCreatedNotification_whenBrokerUnavailable_returnsFalse() {
        // given - stop RabbitMQ container to simulate broker unavailability
        rabbitMQ.stop();

        User user = User.builder()
                .username("offline_user")
                .name("Offline User")
                .email("offline@example.com")
                .password("pass")
                .notificationChannel(NotificationChannelType.TELEGRAM)
                .preferredLanguage(UserLanguageType.EN)
                .telegramUserId(111222L)
                .build();
        Event event = buildTransientEvent(1L, 1L, "Offline Event");

        // when
        boolean result = telegramEventNotificationService.sendEventCreatedNotification(
                event, "Orchestra", user, "Invite text");

        // then - service handles broker failure gracefully without throwing
        assertThat(result).isFalse();
    }

    private Event buildTransientEvent(Long orgId, Long creatorId, String title) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        return Event.builder()
                .id(1L)
                .organizationId(orgId)
                .creatorUserId(creatorId)
                .title(title)
                .eventType(EventType.CONCERT)
                .startTime(start)
                .endTime(start.plus(2, ChronoUnit.HOURS))
                .sendRsvp(true)
                .includeAllOrganizationMembers(false)
                .build();
    }
}
