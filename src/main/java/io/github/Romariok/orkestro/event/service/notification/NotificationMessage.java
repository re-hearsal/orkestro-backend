package io.github.Romariok.orkestro.event.service.notification;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import java.util.Map;

public record NotificationMessage(
        Long recipientUserId,
        NotificationChannelType channel,
        String messageType,
        String text,
        Map<String, Object> extra
) {}
