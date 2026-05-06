package io.github.Romariok.orkestro.event.specification;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventComment;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EventCommentSpecifications {

    private EventCommentSpecifications() {
    }

    public static Specification<EventComment> eventOrganizationEquals(Long organizationId) {
        return (root, query, cb) -> {
            var subquery = query.subquery(Long.class);
            var eventRoot = subquery.from(Event.class);
            subquery.select(eventRoot.get("id"));
            subquery.where(
                    cb.equal(eventRoot.get("id"), root.get("eventId")),
                    cb.equal(eventRoot.get("organizationId"), organizationId));
            return cb.exists(subquery);
        };
    }

    public static Specification<EventComment> eventIntersectsDateRange(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }

            var subquery = query.subquery(Long.class);
            var eventRoot = subquery.from(Event.class);
            subquery.select(eventRoot.get("id"));

            if (from != null && to != null) {
                subquery.where(
                        cb.equal(eventRoot.get("id"), root.get("eventId")),
                        cb.lessThanOrEqualTo(eventRoot.get("startTime"), to),
                        cb.greaterThanOrEqualTo(eventRoot.get("endTime"), from));
            } else if (from != null) {
                subquery.where(
                        cb.equal(eventRoot.get("id"), root.get("eventId")),
                        cb.greaterThanOrEqualTo(eventRoot.get("endTime"), from));
            } else {
                subquery.where(
                        cb.equal(eventRoot.get("id"), root.get("eventId")),
                        cb.lessThanOrEqualTo(eventRoot.get("startTime"), to));
            }

            return cb.exists(subquery);
        };
    }

    public static Specification<EventComment> eventTypeEquals(EventType eventType) {
        return (root, query, cb) -> {
            if (eventType == null) {
                return cb.conjunction();
            }

            var subquery = query.subquery(Long.class);
            var eventRoot = subquery.from(Event.class);
            subquery.select(eventRoot.get("id"));
            subquery.where(
                    cb.equal(eventRoot.get("id"), root.get("eventId")),
                    cb.equal(eventRoot.get("eventType"), eventType));
            return cb.exists(subquery);
        };
    }

    public static Specification<EventComment> eventTitleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return cb.conjunction();
            }

            String likePattern = "%" + title.trim().toLowerCase(Locale.ROOT) + "%";
            var subquery = query.subquery(Long.class);
            var eventRoot = subquery.from(Event.class);
            subquery.select(eventRoot.get("id"));
            subquery.where(
                    cb.equal(eventRoot.get("id"), root.get("eventId")),
                    cb.like(cb.lower(eventRoot.get("title")), likePattern));
            return cb.exists(subquery);
        };
    }

    public static Specification<EventComment> eventHasAnyTag(Collection<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) {
                return cb.conjunction();
            }

            List<String> normalizedTags = tags.stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();

            if (normalizedTags.isEmpty()) {
                return cb.conjunction();
            }

            var subquery = query.subquery(Long.class);
            var eventRoot = subquery.from(Event.class);
            var tagsJoin = eventRoot.join("tags");
            subquery.select(eventRoot.get("id"));
            subquery.where(
                    cb.equal(eventRoot.get("id"), root.get("eventId")),
                    cb.lower(tagsJoin.as(String.class)).in(normalizedTags));
            return cb.exists(subquery);
        };
    }

    public static Specification<EventComment> eventInvolvesUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.disjunction();
            }

            var subquery = query.subquery(Long.class);
            var participantRoot = subquery.from(EventParticipant.class);
            subquery.select(participantRoot.get("eventId"));
            subquery.where(
                    cb.equal(participantRoot.get("eventId"), root.get("eventId")),
                    cb.equal(participantRoot.get("userId"), userId));

            var eventSubquery = query.subquery(Long.class);
            var eventRoot = eventSubquery.from(Event.class);
            eventSubquery.select(eventRoot.get("id"));
            eventSubquery.where(
                    cb.equal(eventRoot.get("id"), root.get("eventId")),
                    cb.equal(eventRoot.get("creatorUserId"), userId));

            return cb.or(cb.exists(subquery), cb.exists(eventSubquery));
        };
    }
}
