package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventAttendanceMarkRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.service.EventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventAttendanceControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventAttendanceController eventAttendanceController;

    @Test
    void markEventAttendance_returnsNoContent() {
        doNothing().when(eventService).markEventAttendance(eq(1L), eq(10L), eq(5L), eq(EventAttendanceStatus.ATTENDED));

        EventAttendanceMarkRequestDTO request = EventAttendanceMarkRequestDTO.builder()
                .participantUserId(5L)
                .attendanceStatus(EventAttendanceStatus.ATTENDED)
                .build();

        var result = eventAttendanceController.markEventAttendance(1L, 10L, request);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void getAttendanceMatrix_returnsOk() {
        List<EventAttendanceRowDTO> matrix = List.of(
                EventAttendanceRowDTO.builder()
                        .name("Ivan Petrov")
                        .rsvpStatus(EventRsvpStatus.ACCEPTED)
                        .attendanceStatus(EventAttendanceStatus.ATTENDED)
                        .build(),
                EventAttendanceRowDTO.builder()
                        .name("Anna Sidorova")
                        .rsvpStatus(EventRsvpStatus.DECLINED)
                        .attendanceStatus(EventAttendanceStatus.ABSENT)
                        .build()
        );
        when(eventService.getEventAttendanceTable(eq(1L), eq(10L))).thenReturn(matrix);

        var result = eventAttendanceController.getAttendanceMatrix(1L, 10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals("Ivan Petrov", result.getBody().get(0).getName());
    }

    @Test
    void exportAttendanceMatrixCsv_returnsOkWithCsvContentType() {
        String csv = "Name,RSVP,Attendance\nIvan Petrov,ACCEPTED,ATTENDED\n";
        when(eventService.exportEventAttendanceMatrixAsCsv(eq(1L), eq(10L))).thenReturn(csv);

        var result = eventAttendanceController.exportAttendanceMatrixCsv(1L, 10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("text/csv", result.getHeaders().getContentType().toString());
    }
}
