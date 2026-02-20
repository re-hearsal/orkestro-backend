package io.github.Romariok.orkestro.event.specification;

import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventSection;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> organizationEquals(Long organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
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

    public static Specification<Event> includeAllOrganizationMembers() {
        return (root, query, cb) -> cb.isTrue(root.get("includeAllOrganizationMembers"));
    }

    public static Specification<Event> hasAnySection(Collection<Long> sectionIds) {
        return (root, query, cb) -> {
            if (sectionIds == null || sectionIds.isEmpty()) {
                return cb.disjunction();
            }

            var subquery = query.subquery(Long.class);
            var eventSectionRoot = subquery.from(EventSection.class);
            subquery.select(eventSectionRoot.get("eventId"));
            subquery.where(
                    cb.equal(eventSectionRoot.get("eventId"), root.get("id")),
                    eventSectionRoot.get("sectionId").in(sectionIds));
            return cb.exists(subquery);
        };
    }

    public static Specification<Event> hasParticipantUser(Long userId) {
        return (root, query, cb) -> {
            if (userId == null) {
                return cb.disjunction();
            }

            var subquery = query.subquery(Long.class);
            var participantRoot = subquery.from(EventParticipant.class);
            subquery.select(participantRoot.get("eventId"));
            subquery.where(
                    cb.equal(participantRoot.get("eventId"), root.get("id")),
                    cb.equal(participantRoot.get("userId"), userId));
            return cb.exists(subquery);
        };
    }
}
