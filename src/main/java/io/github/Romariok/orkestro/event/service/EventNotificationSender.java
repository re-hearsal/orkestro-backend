package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.user.models.User;

/**
 * Common interface for sending event-related notifications
 * (e.g., via Telegram or email).
 */
public interface EventNotificationSender {

    /**
     * Send a notification to a single user about a newly created event.
     *
     * @param event            the event that was created
     * @param organizationName the name of the organization that owns the event
     * @param user             the target user
     * @param text             pre-built human-readable invitation text
     * @return {@code true} if notification was sent successfully,
     *         {@code false} otherwise
     */
    boolean sendEventCreatedNotification(Event event, String organizationName, User user, String text);
}
