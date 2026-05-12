package io.github.Romariok.orkestro.event.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmailNotificationMessage(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("event_id") Long eventId,
        @JsonProperty("to") String to,
        @JsonProperty("subject") String subject,
        @JsonProperty("text") String text,
        @JsonProperty("organization_name") String organizationName,
        @JsonProperty("event_title") String eventTitle,
        @JsonProperty("include_rsvp_form") Boolean includeRsvpForm,
        @JsonProperty("template") String template,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("section_name") String sectionName) {
}
