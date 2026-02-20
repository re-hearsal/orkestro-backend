package io.github.Romariok.orkestro.event.dto;

import java.time.Duration;
import java.time.Instant;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class EventUserCalendarRequestDTO {
    private static final long MAX_RANGE_DAYS = 92;

    private Instant from;
    private Instant to;

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
