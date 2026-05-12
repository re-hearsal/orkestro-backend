package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventDuplicateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @Test
    void createEvent_returnsCreated() {
        EventDTO response = EventDTO.builder()
                .id(1L)
                .organizationId(1L)
                .title("New Year Concert")
                .eventType(EventType.CONCERT)
                .startTime(Instant.parse("2026-12-31T19:00:00Z"))
                .endTime(Instant.parse("2026-12-31T23:00:00Z"))
                .build();
        when(eventService.createEventInOrganization(eq(1L), any(EventCreateRequestDTO.class))).thenReturn(response);

        EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                .title("New Year Concert")
                .eventType(EventType.CONCERT)
                .startTime(Instant.parse("2026-12-31T19:00:00Z"))
                .endTime(Instant.parse("2026-12-31T23:00:00Z"))
                .build();

        var result = eventController.createEvent(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("New Year Concert", result.getBody().getTitle());
    }

    @Test
    void updateEvent_returnsOk() {
        EventDTO response = EventDTO.builder()
                .id(5L)
                .organizationId(1L)
                .title("Updated Concert")
                .eventType(EventType.CONCERT)
                .build();
        when(eventService.updateEvent(eq(1L), eq(5L), any(EventUpdateRequestDTO.class))).thenReturn(response);

        var result = eventController.updateEvent(1L, 5L, new EventUpdateRequestDTO());

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5L, result.getBody().getId());
        assertEquals("Updated Concert", result.getBody().getTitle());
    }

    @Test
    void deleteEvent_returnsNoContent() {
        doNothing().when(eventService).deleteEvent(1L, 5L);

        var result = eventController.deleteEvent(1L, 5L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void duplicateEvent_returnsCreated() {
        List<EventDTO> response = List.of(
                EventDTO.builder().id(10L).organizationId(1L).title("Concert Copy").build(),
                EventDTO.builder().id(11L).organizationId(1L).title("Concert Copy").build()
        );
        when(eventService.duplicateEvent(eq(1L), eq(5L), any())).thenReturn(response);

        EventDuplicateRequestDTO request = EventDuplicateRequestDTO.builder()
                .startTimes(List.of(
                        Instant.parse("2027-01-15T19:00:00Z"),
                        Instant.parse("2027-02-15T19:00:00Z")
                ))
                .build();

        var result = eventController.duplicateEvent(1L, 5L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }
}
