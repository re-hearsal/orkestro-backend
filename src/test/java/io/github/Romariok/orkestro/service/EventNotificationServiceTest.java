package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.service.EmailEventNotificationService;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.event.service.TelegramEventNotificationService;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private TelegramEventNotificationService telegramEventNotificationService;

        @Mock
        private EmailEventNotificationService emailEventNotificationService;

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
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                Long telegramUserId = 1000L;

                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(telegramUserId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
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
                when(telegramEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(true);

                eventNotificationService.sendEventReminderNotifications(event, List.of(userId));

                verify(telegramEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService, never())
                                .sendEventCreatedNotification(any(Event.class), any(), any(User.class), any());
        }

        @Test
        void sendEventReminderNotifications_telegramFailure_fallsBackToEmail() {
                Event event = Event.builder()
                                .id(1L)
                                .organizationId(1L)
                                .title("Concert")
                                .createdAt(Instant.now())
                                .build();

                Long userId = 42L;
                Long telegramUserId = 1000L;

                User user = User.builder()
                                .id(userId)
                                .username("user")
                                .telegramUserId(telegramUserId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
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
                when(telegramEventNotificationService
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any()))
                                .thenReturn(false);

                eventNotificationService.sendEventReminderNotifications(event, List.of(userId));

                verify(telegramEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
                verify(emailEventNotificationService)
                                .sendEventCreatedNotification(eq(event), eq("Orchestra"), eq(user), any());
        }
}
