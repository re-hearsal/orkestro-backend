package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventCalendarGroupedResponseDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventUserCalendarRequestDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventQueryControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventQueryController eventQueryController;

    @Test
    void getEvent_returnsOk() {
        EventDTO response = EventDTO.builder()
                .id(10L)
                .organizationId(1L)
                .title("Rehearsal")
                .eventType(EventType.REHEARSAL)
                .startTime(Instant.parse("2026-03-01T10:00:00Z"))
                .build();
        when(eventService.getEventForCurrentUser(eq(1L), eq(10L))).thenReturn(response);

        var result = eventQueryController.getEvent(1L, 10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertEquals("Rehearsal", result.getBody().getTitle());
    }

    @Test
    void getOrganizationCalendar_returnsOk() {
        EventCalendarGroupedResponseDTO response = EventCalendarGroupedResponseDTO.builder()
                .page(0)
                .size(50)
                .totalElements(5)
                .totalPages(1)
                .first(true)
                .last(true)
                .sectionGroups(List.of())
                .organizationWideEvents(List.of())
                .build();
        when(eventService.getCalendarForCurrentUserInOrganization(eq(1L), any(EventCalendarRequestDTO.class), any()))
                .thenReturn(response);

        var result = eventQueryController.getOrganizationCalendar(1L, new EventCalendarRequestDTO(), PageRequest.of(0, 50));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5, result.getBody().getTotalElements());
    }

    @Test
    void getCurrentUserCalendar_returnsOk() {
        EventCalendarGroupedResponseDTO response = EventCalendarGroupedResponseDTO.builder()
                .page(0)
                .size(50)
                .totalElements(3)
                .totalPages(1)
                .first(true)
                .last(true)
                .sectionGroups(List.of())
                .organizationWideEvents(List.of())
                .build();
        when(eventService.getCurrentUserCalendarInOrganization(eq(1L), any(EventUserCalendarRequestDTO.class), any()))
                .thenReturn(response);

        var result = eventQueryController.getCurrentUserCalendar(1L, new EventUserCalendarRequestDTO(), PageRequest.of(0, 50));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().getTotalElements());
    }

    @Test
    void getEventTags_returnsOk() {
        List<String> tags = List.of("concert", "outdoor", "new-year");
        when(eventService.getOrganizationEventTags(eq(1L))).thenReturn(tags);

        var result = eventQueryController.getEventTags(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().size());
        assertEquals("concert", result.getBody().get(0));
    }
}
