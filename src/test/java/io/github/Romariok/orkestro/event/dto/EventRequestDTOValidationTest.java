package io.github.Romariok.orkestro.event.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.Romariok.orkestro.event.models.enums.EventType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventRequestDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EventCreateRequestDTO validCreate() {
        return EventCreateRequestDTO.builder()
                .title("Rehearsal")
                .eventType(EventType.REHEARSAL)
                .startTime(Instant.parse("2030-01-01T10:00:00Z"))
                .endTime(Instant.parse("2030-01-01T12:00:00Z"))
                .build();
    }

    private EventUpdateRequestDTO validUpdate() {
        return EventUpdateRequestDTO.builder().build();
    }

    private <T> boolean hasViolationOn(Set<ConstraintViolation<T>> violations, String field) {
        return violations.stream().anyMatch(v -> field.equals(v.getPropertyPath().toString()));
    }

    // ---- EventCreateRequestDTO ----

    @Test
    void create_validDto_noViolations() {
        assertTrue(validator.validate(validCreate()).isEmpty());
    }

    @Test
    void create_titleTooLong_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setTitle("a".repeat(31));
        assertTrue(hasViolationOn(validator.validate(dto), "title"));
    }

    @Test
    void create_titleExactlyMaxLength_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setTitle("a".repeat(30));
        assertFalse(hasViolationOn(validator.validate(dto), "title"));
    }

    @Test
    void create_titleBlank_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setTitle("   ");
        assertTrue(hasViolationOn(validator.validate(dto), "title"));
    }

    @Test
    void create_descriptionTooLong_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setDescription("a".repeat(2001));
        assertTrue(hasViolationOn(validator.validate(dto), "description"));
    }

    @Test
    void create_descriptionExactlyMaxLength_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setDescription("a".repeat(2000));
        assertFalse(hasViolationOn(validator.validate(dto), "description"));
    }

    @Test
    void create_tooManyTags_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setTags(List.of("a", "b", "c", "d", "e", "f"));
        assertTrue(hasViolationOn(validator.validate(dto), "tags"));
    }

    @Test
    void create_fiveTagsAllowed_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setTags(List.of("a", "b", "c", "d", "e"));
        assertFalse(hasViolationOn(validator.validate(dto), "tags"));
    }

    @Test
    void create_remindBeforeMinutesTooHigh_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setRemindBeforeMinutes(1441);
        assertTrue(hasViolationOn(validator.validate(dto), "remindBeforeMinutes"));
    }

    @Test
    void create_remindBeforeMinutesMax_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setRemindBeforeMinutes(1440);
        assertFalse(hasViolationOn(validator.validate(dto), "remindBeforeMinutes"));
    }

    @Test
    void create_remindBeforeMinutesNegative_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setRemindBeforeMinutes(-1);
        assertTrue(hasViolationOn(validator.validate(dto), "remindBeforeMinutes"));
    }

    @Test
    void create_startTimeInPast_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setStartTime(Instant.now().minusSeconds(60));
        assertTrue(hasViolationOn(validator.validate(dto), "startTime"));
    }

    @Test
    void create_startTimeInFuture_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setStartTime(Instant.now().plusSeconds(3600));
        assertFalse(hasViolationOn(validator.validate(dto), "startTime"));
    }

    @Test
    void create_tooManySongs_violation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setSongIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
                21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,
                31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L,
                41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L));
        assertTrue(hasViolationOn(validator.validate(dto), "songIds"));
    }

    @Test
    void create_fiftySongsAllowed_noViolation() {
        EventCreateRequestDTO dto = validCreate();
        dto.setSongIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
                21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,
                31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L,
                41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L));
        assertFalse(hasViolationOn(validator.validate(dto), "songIds"));
    }

    // ---- EventUpdateRequestDTO ----

    @Test
    void update_emptyDto_noViolations() {
        assertTrue(validator.validate(validUpdate()).isEmpty());
    }

    @Test
    void update_titleTooLong_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setTitle("a".repeat(31));
        assertTrue(hasViolationOn(validator.validate(dto), "title"));
    }

    @Test
    void update_titleNull_noViolation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setTitle(null);
        assertFalse(hasViolationOn(validator.validate(dto), "title"));
    }

    @Test
    void update_descriptionTooLong_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setDescription("a".repeat(2001));
        assertTrue(hasViolationOn(validator.validate(dto), "description"));
    }

    @Test
    void update_tooManyTags_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setTags(List.of("a", "b", "c", "d", "e", "f"));
        assertTrue(hasViolationOn(validator.validate(dto), "tags"));
    }

    @Test
    void update_remindBeforeMinutesTooHigh_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setRemindBeforeMinutes(1441);
        assertTrue(hasViolationOn(validator.validate(dto), "remindBeforeMinutes"));
    }

    @Test
    void update_startTimeInPast_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setStartTime(Instant.now().minusSeconds(60));
        assertTrue(hasViolationOn(validator.validate(dto), "startTime"));
    }

    @Test
    void update_startTimeNull_noViolation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setStartTime(null);
        assertFalse(hasViolationOn(validator.validate(dto), "startTime"));
    }

    @Test
    void update_remindBeforeMinutesNegative_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setRemindBeforeMinutes(-1);
        assertTrue(hasViolationOn(validator.validate(dto), "remindBeforeMinutes"));
    }

    @Test
    void update_tooManySongs_violation() {
        EventUpdateRequestDTO dto = validUpdate();
        dto.setSongIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
                11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L,
                21L, 22L, 23L, 24L, 25L, 26L, 27L, 28L, 29L, 30L,
                31L, 32L, 33L, 34L, 35L, 36L, 37L, 38L, 39L, 40L,
                41L, 42L, 43L, 44L, 45L, 46L, 47L, 48L, 49L, 50L, 51L));
        assertTrue(hasViolationOn(validator.validate(dto), "songIds"));
    }
}
