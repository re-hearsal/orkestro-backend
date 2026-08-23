package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventNotificationService {

    private static final ZoneId EVENT_DISPLAY_ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter EVENT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(EVENT_DISPLAY_ZONE);
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm").withZone(EVENT_DISPLAY_ZONE);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final TelegramEventNotificationService telegramEventNotificationService;
    private final VkEventNotificationService vkEventNotificationService;
    private final EmailEventNotificationService emailEventNotificationService;
    private final MessageSource messageSource;

    @Value("${orkestro.frontend.base-url}")
    private String frontendBaseUrl;

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
                .orElse(null);

        for (User user : users) {
            try {
                sendCreatedNotificationToUser(event, organizationName, user);
            } catch (Exception e) {
                log.error(
                        "Failed to send event created notification for event {} to user {}",
                        event.getId(),
                        user.getId(),
                        e);
            }
        }
    }

    public void sendEventReminderNotifications(Event event, Collection<Long> participantUserIds) {
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
                .orElse(null);

        for (User user : users) {
            try {
                sendReminderToUser(event, organizationName, user);
            } catch (Exception e) {
                log.error(
                        "Failed to send event reminder for event {} to user {}",
                        event.getId(),
                        user.getId(),
                        e);
            }
        }
    }

    public void sendEventCommentNotifications(
            Event event, Collection<Long> participantUserIds, String authorName, String commentText) {
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
                .orElse(null);

        for (User user : users) {
            try {
                sendCommentToUser(event, organizationName, user, authorName, commentText);
            } catch (Exception e) {
                log.error(
                        "Failed to send event comment notification for event {} to user {}",
                        event.getId(),
                        user.getId(),
                        e);
            }
        }
    }

    private void sendCreatedNotificationToUser(Event event, String organizationName, User user) {
        UserLanguageType language = resolveLanguage(user);
        String resolvedOrganizationName = resolveOrganizationName(organizationName, language);
        String text = buildInviteText(event, resolvedOrganizationName, language);
        NotificationChannelType channel = user.getNotificationChannel();

        if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
            boolean telegramOk = telegramEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName,
                    user, text);
            if (!telegramOk) {
                emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            }
        } else if (channel == NotificationChannelType.VK && user.getVkUserId() != null) {
            boolean vkOk = vkEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            if (!vkOk) {
                emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            }
        } else {
            emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
        }
    }

    private void sendReminderToUser(Event event, String organizationName, User user) {
        UserLanguageType language = resolveLanguage(user);
        String resolvedOrganizationName = resolveOrganizationName(organizationName, language);
        String text = buildReminderText(event, resolvedOrganizationName, language);
        NotificationChannelType channel = user.getNotificationChannel();

        if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
            boolean telegramOk = telegramEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName,
                    user, text);
            if (!telegramOk) {
                emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            }
        } else if (channel == NotificationChannelType.VK && user.getVkUserId() != null) {
            boolean vkOk = vkEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            if (!vkOk) {
                emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
            }
        } else {
            emailEventNotificationService.sendEventCreatedNotification(event, resolvedOrganizationName, user, text);
        }
    }

    private String buildInviteText(Event event, String organizationName, UserLanguageType language) {
        Locale locale = toLocale(language);
        String title = event.getTitle() != null ? event.getTitle() : defaultTitle(language);
        String date = formatEventDate(event.getStartTime(), language);
        String time = formatEventTime(event.getStartTime(), language);
        String location = resolveLocation(event.getLocation(), language);
        String description = resolveDescription(event.getDescription(), language);
        String link = buildEventLink(event);
        return getMessage(
                "notification.event.created.text", locale, title, organizationName, date, time, location, description, link);
    }

    private String buildReminderText(Event event, String organizationName, UserLanguageType language) {
        Locale locale = toLocale(language);
        String title = event.getTitle() != null ? event.getTitle() : defaultTitle(language);
        String date = formatEventDate(event.getStartTime(), language);
        String time = formatEventTime(event.getStartTime(), language);
        String location = resolveLocation(event.getLocation(), language);
        String description = resolveDescription(event.getDescription(), language);
        String link = buildEventLink(event);
        return getMessage(
                "notification.event.reminder.text", locale, title, organizationName, date, time, location, description, link);
    }

    private String buildCommentText(
            Event event,
            String organizationName,
            UserLanguageType language,
            String authorName,
            String commentText) {
        Locale locale = toLocale(language);
        String title = event.getTitle() != null ? event.getTitle() : defaultTitle(language);
        String resolvedAuthorName = authorName != null && !authorName.isBlank()
                ? authorName
                : getMessage("notification.event.comment.author.fallback", locale);
        String resolvedCommentText = commentText != null && !commentText.isBlank()
                ? commentText
                : getMessage("notification.event.comment.text.fallback", locale);
        return getMessage(
                "notification.event.comment.text",
                locale,
                title,
                organizationName,
                resolvedAuthorName,
                resolvedCommentText);
    }

    private void sendCommentToUser(
            Event event, String organizationName, User user, String authorName, String commentText) {
        UserLanguageType language = resolveLanguage(user);
        String resolvedOrganizationName = resolveOrganizationName(organizationName, language);
        String text = buildCommentText(event, resolvedOrganizationName, language, authorName, commentText);
        NotificationChannelType channel = user.getNotificationChannel();

        if (channel == NotificationChannelType.TELEGRAM && user.getTelegramUserId() != null) {
            boolean telegramOk = telegramEventNotificationService.sendEventCommentNotification(
                    event, resolvedOrganizationName, user, text);
            if (!telegramOk) {
                emailEventNotificationService.sendEventCommentNotification(event, resolvedOrganizationName, user, text);
            }
        } else if (channel == NotificationChannelType.VK && user.getVkUserId() != null) {
            boolean vkOk = vkEventNotificationService.sendEventCommentNotification(event, resolvedOrganizationName, user, text);
            if (!vkOk) {
                emailEventNotificationService.sendEventCommentNotification(event, resolvedOrganizationName, user, text);
            }
        } else {
            emailEventNotificationService.sendEventCommentNotification(event, resolvedOrganizationName, user, text);
        }
    }

    private UserLanguageType resolveLanguage(User user) {
        if (user == null || user.getPreferredLanguage() == null) {
            return UserLanguageType.RU;
        }
        return user.getPreferredLanguage();
    }

    private String defaultTitle(UserLanguageType language) {
        return getMessage("notification.event.title.untitled", toLocale(language));
    }

    private String defaultStartTime(UserLanguageType language) {
        return getMessage("notification.event.start-time.not-specified", toLocale(language));
    }

    private String resolveOrganizationName(String organizationName, UserLanguageType language) {
        if (organizationName != null && !organizationName.isBlank()) {
            return organizationName;
        }
        return getMessage("notification.event.organization.fallback", toLocale(language));
    }

    private String formatEventDate(Instant startTime, UserLanguageType language) {
        return startTime != null ? EVENT_DATE_FORMATTER.format(startTime) : defaultStartTime(language);
    }

    private String formatEventTime(Instant startTime, UserLanguageType language) {
        return startTime != null ? EVENT_TIME_FORMATTER.format(startTime) : defaultStartTime(language);
    }

    private String resolveLocation(String location, UserLanguageType language) {
        if (location != null && !location.isBlank()) {
            return location;
        }
        return getMessage("notification.event.location.not-specified", toLocale(language));
    }

    private String resolveDescription(String description, UserLanguageType language) {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return getMessage("notification.event.description.not-specified", toLocale(language));
    }

    private String buildEventLink(Event event) {
        return frontendBaseUrl + "/organizations/" + event.getOrganizationId() + "/events/" + event.getId();
    }

    private Locale toLocale(UserLanguageType language) {
        return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }

    private String getMessage(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, locale);
    }

}
