package io.github.Romariok.orkestro.event.dto;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EventCalendarRequestDTO {
    private static final int MAX_SECTION_IDS = 20;
    private static final long MAX_RANGE_DAYS = 92;

    private String scope;
    @Positive(message = "sectionId must be a positive number")
    private Long sectionId;
    private List<@Positive(message = "sectionIds must contain only positive numbers") Long> sectionIds;
    private Instant from;
    private Instant to;
    private Boolean includeOrgWide;

    public EventCalendarScope scopeAsEnum() {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        try {
            return EventCalendarScope.valueOf(scope.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public List<Long> normalizedSectionIds() {
        if (sectionIds == null) {
            return List.of();
        }
        return sectionIds.stream()
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    @AssertTrue(message = "scope must be one of: section, sections, organization")
    public boolean isScopeValid() {
        return scopeAsEnum() != null;
    }

    @AssertTrue(message = "sectionId must be provided for SECTION scope")
    public boolean isSectionIdProvidedForSectionScope() {
        EventCalendarScope parsed = scopeAsEnum();
        if (parsed != EventCalendarScope.SECTION) {
            return true;
        }
        return sectionId != null;
    }

    @AssertTrue(message = "sectionIds must be provided for SECTIONS scope")
    public boolean isSectionIdsProvidedForSectionsScope() {
        EventCalendarScope parsed = scopeAsEnum();
        if (parsed != EventCalendarScope.SECTIONS) {
            return true;
        }
        return normalizedSectionIds().size() > 0;
    }

    @AssertTrue(message = "sectionIds size must be <= " + MAX_SECTION_IDS)
    public boolean isSectionIdsSizeValid() {
        EventCalendarScope parsed = scopeAsEnum();
        if (parsed != EventCalendarScope.SECTIONS) {
            return true;
        }
        return normalizedSectionIds().size() <= MAX_SECTION_IDS;
    }

    @AssertTrue(message = "to must be after from")
    public boolean isDateOrderValid() {
        if (from == null || to == null) {
            return true;
        }
        return to.isAfter(from);
    }

    @AssertTrue(message = "Date window must be <= " + MAX_RANGE_DAYS + " days")
    public boolean isDateRangeValid() {
        if (from == null || to == null) {
            return true;
        }
        return Duration.between(from, to).compareTo(Duration.ofDays(MAX_RANGE_DAYS)) <= 0;
    }
}
