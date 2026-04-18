package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.service.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventExportControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventExportController eventExportController;

    @Test
    void exportScheduleAsIcs_returnsOkWithCalendarContentType() {
        String icsContent = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR";
        when(eventService.exportCurrentUserScheduleAsIcal(eq(false))).thenReturn(icsContent);

        var result = eventExportController.exportScheduleAsIcs(false);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("text/calendar", result.getHeaders().getContentType().toString());
    }

    @Test
    void exportScheduleAsIcs_withIncludeDeclined_returnsOk() {
        String icsContent = "BEGIN:VCALENDAR\nVERSION:2.0\nEND:VCALENDAR";
        when(eventService.exportCurrentUserScheduleAsIcal(eq(true))).thenReturn(icsContent);

        var result = eventExportController.exportScheduleAsIcs(true);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void exportScheduleAsCsv_returnsOkWithCsvContentType() {
        String csvContent = "Title,Start,End\nConcert,2026-12-31T19:00:00Z,2026-12-31T23:00:00Z\n";
        when(eventService.exportCurrentUserScheduleAsCsv(eq(false))).thenReturn(csvContent);

        var result = eventExportController.exportScheduleAsCsv(false);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("text/csv", result.getHeaders().getContentType().toString());
    }
}
