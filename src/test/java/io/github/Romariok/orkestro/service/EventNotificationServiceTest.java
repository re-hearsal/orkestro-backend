package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.service.EmailEventNotificationService;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.event.service.TelegramEventNotificationService;
import io.github.Romariok.orkestro.event.service.VkEventNotificationService;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private TelegramEventNotificationService telegramEventNotificationService;

        @Mock
        private VkEventNotificationService vkEventNotificationService;

        @Mock
        private EmailEventNotificationService emailEventNotificationService;

        @Mock
        private MessageSource messageSource;

        @InjectMocks
        private EventNotificationService eventNotificationService;

        @Test
        void sendEventReminderNotifications_emptyRecipients_doesNothing() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Event")
                                .createdAt(Instant.now())
                                .build();

                eventNotificationService.sendEventReminderNotifications(event, List.of());

                verify(userRepository, never()).findAllById(anyCollection());
                verify(telegramEventNotificationService, never())
                                .sendEventCreatedNotification(any(Event.class), any(), any(User.class), any());
                verify(emailEventNotificationService, never())
                                .sendEventCreatedNotification(any(Event.class), any(), any(User.class), any());
        }

        @Test
        void sendEventReminderNotifications_telegramPreferred_usesTelegramAndNoEmailOnSuccess() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                Long telegramUserId = 1000L;

                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(telegramUserId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .profileImageFileId(1L)
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Reminder about the event");
                when(telegramEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventReminderNotifications(event, List.of(userId));

                verify(telegramEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                text -> text != null && text.contains("Reminder about the event")));
                verify(emailEventNotificationService, never())
                                .sendEventCreatedNotification(any(Event.class), any(), any(User.class), any());
        }

        @Test
        void sendEventReminderNotifications_telegramFailure_fallsBackToEmail() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                Long telegramUserId = 1000L;

                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(telegramUserId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .profileImageFileId(1L)
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Reminder about the event");
                when(telegramEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(false);

                eventNotificationService.sendEventReminderNotifications(event, List.of(userId));

                verify(telegramEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                text -> text != null && text.contains("Reminder about the event")));
                verify(emailEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                text -> text != null && text.contains("Reminder about the event")));
        }

        @Test
        void sendEventReminderNotifications_userWithEmailChannelNoMessenger_usesEmailDirectly() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;

                User user = User.builder()
                                .id(userId)
                                .username("email-only-user")
                                .telegramUserId(null)
                                .vkUserId(null)
                                .notificationChannel(NotificationChannelType.EMAIL)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(null)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Reminder text");

                eventNotificationService.sendEventReminderNotifications(event, List.of(userId));

                verify(emailEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(telegramEventNotificationService, never())
                                .sendEventCreatedNotification(any(), any(), any(), any());
                verify(vkEventNotificationService, never())
                                .sendEventCreatedNotification(any(), any(), any(), any());
        }

        @Test
        void sendEventCommentNotifications_telegramPreferred_usesCommentChannelMethod() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                Long telegramUserId = 1000L;

                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(telegramUserId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .profileImageFileId(1L)
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("New comment");
                when(telegramEventNotificationService
                                .sendEventCommentNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventCommentNotifications(event, List.of(userId), "Author", "Comment body");

                verify(telegramEventNotificationService)
                                .sendEventCommentNotification(eq(event), eq("Orchestra"), eq(user),
                                                org.mockito.ArgumentMatchers.argThat(
                                                                text -> text != null && text.contains("New comment")));
                verify(emailEventNotificationService, never())
                                .sendEventCommentNotification(any(Event.class), any(), any(User.class), any());
        }

        @Test
        void sendEventCreatedNotifications_emptyRecipients_doesNothing() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Event")
                                .createdAt(Instant.now())
                                .build();

                eventNotificationService.sendEventCreatedNotifications(event, List.of());

                verify(userRepository, never()).findAllById(anyCollection());
        }

        @Test
        void sendEventCreatedNotifications_telegramChannel_success_usesTelegramNotEmail() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(1000L)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .profileImageFileId(1L)
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Event created notification");
                when(telegramEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventCreatedNotifications(event, List.of(userId));

                verify(telegramEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService, never())
                                .sendEventCreatedNotification(any(), any(), any(), any());
        }

        @Test
        void sendEventCreatedNotifications_vkChannel_success_usesVkNotEmail() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 55L;
                User user = User.builder()
                                .id(userId)
                                .username("vkUser")
                                .vkUserId(9999L)
                                .notificationChannel(NotificationChannelType.VK)
                                .preferredLanguage(UserLanguageType.RU)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Event created");
                when(vkEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventCreatedNotifications(event, List.of(userId));

                verify(vkEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService, never())
                                .sendEventCreatedNotification(any(), any(), any(), any());
        }

        @Test
        void sendEventCreatedNotifications_vkChannelFailure_fallsBackToEmail() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 55L;
                User user = User.builder()
                                .id(userId)
                                .username("vkUser")
                                .vkUserId(9999L)
                                .notificationChannel(NotificationChannelType.VK)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("Event notification text");
                when(vkEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(false);

                eventNotificationService.sendEventCreatedNotifications(event, List.of(userId));

                verify(vkEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
        }

        @Test
        void sendEventCommentNotifications_vkChannel_usesVkCommentMethod() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .startTime(Instant.now())
                                .createdAt(Instant.now())
                                .build();

                Long userId = 55L;
                User user = User.builder()
                                .id(userId)
                                .username("vkUser")
                                .vkUserId(9999L)
                                .notificationChannel(NotificationChannelType.VK)
                                .preferredLanguage(UserLanguageType.EN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Organization organization = Organization.builder()
                                .id(1L)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(1L)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
                when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
                when(messageSource.getMessage(anyString(), org.mockito.ArgumentMatchers.<Object[]>any(), any(Locale.class)))
                                .thenReturn("New comment");
                when(vkEventNotificationService
                                .sendEventCommentNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventCommentNotifications(event, List.of(userId), "Author", "Comment body");

                verify(vkEventNotificationService)
                                .sendEventCommentNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService, never())
                                .sendEventCommentNotification(any(), any(), any(), any());
        }
}
