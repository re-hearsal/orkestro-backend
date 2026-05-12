package io.github.Romariok.orkestro.event.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class EventUserCalendarRequestDTO {
    private static final long MAX_RANGE_DAYS = 92;

    private Instant from;
    private Instant to;
    private String title;
    private List<String> tags;

    public String normalizedTitle() {
        if (title == null) {
            return null;
        }
        String normalized = title.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public List<String> normalizedTags() {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
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
