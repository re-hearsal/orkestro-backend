package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Отправка уведомлений участникам события о создании мероприятия.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final TelegramEventNotificationService telegramEventNotificationService;
    private final EmailEventNotificationService emailEventNotificationService;

    public void sendEventCreatedNotifications(Event event, Collection<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>(participantUserIds);
        List<User> users = userRepository.findAllById(uniqueIds);
        if (users.isEmpty()) {
            return;
        }

        String organizationName = organizationRepository.findById(event.getOrganizationId())
                .map(Organization::getName)
                .orElse("организации");

        for (User user : users) {
            try {
                sendToUser(event, organizationName, user);
            } catch (Exception e) {
                log.error(
                        "Failed to send event created notification for event {} to user {}",
                        event.getId(),
                        user.getId(),
                        e);
            }
        }
    }

    private void sendToUser(Event event, String organizationName, User user) {
        String text = buildInviteText(event, organizationName);
        NotificationChannelType channel = user.getNotificationChannel();

        if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
            boolean telegramOk = telegramEventNotificationService.sendEventCreatedNotification(event, organizationName,
                    user, text);
            if (!telegramOk) {
                emailEventNotificationService.sendEventCreatedNotification(event, organizationName, user, text);
            }
        } else {
            emailEventNotificationService.sendEventCreatedNotification(event, organizationName, user, text);
        }
    }

    private String buildInviteText(Event event, String organizationName) {
        String title = event.getTitle() != null ? event.getTitle() : "без названия";
        return "Вас пригласили на мероприятие \"" + title + "\" в организации \"" + organizationName
                + "\". Просим вас сообщить о вашем присутствии.";
    }

}
