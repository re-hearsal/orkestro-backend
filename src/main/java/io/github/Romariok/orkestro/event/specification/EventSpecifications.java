package io.github.Romariok.orkestro.event.specification;

import io.github.Romariok.orkestro.event.models.Event;
import java.time.Instant;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> organizationEquals(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Event> titleContainsIgnoreCase(String rawTitle) {
        return (root, query, cb) -> {
            if (rawTitle == null) {
                return cb.conjunction();
            }
            String normalized = rawTitle.trim();
            if (normalized.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(root.get("title")), like);
        };
    }

    public static Specification<Event> hasAllTags(Collection<String> tags) {
        return (root, query, cb) -> {
            if (tags == null || tags.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(tags.stream()
                    .map(tag -> cb.isMember(tag, root.get("tags")))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<Event> intersectsDateRange(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return cb.conjunction();
            }
            if (from != null && to != null) {
                return cb.and(
                        cb.lessThanOrEqualTo(root.get("startTime"), to),
                        cb.greaterThanOrEqualTo(root.get("endTime"), from));
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("endTime"), from);
            }
            return cb.lessThanOrEqualTo(root.get("startTime"), to);
        };
    }
}
