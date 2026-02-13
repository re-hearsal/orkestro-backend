package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventAttendanceMarkRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
public class EventAttendanceController {

    private final EventService eventService;

    @PostMapping("/{eventId}/attendance")
    public ResponseEntity<Void> markEventAttendance(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody EventAttendanceMarkRequestDTO request) {
        eventService.markEventAttendance(
                organizationId,
                eventId,
                request.getParticipantUserId(),
                request.getAttendanceStatus());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/attendance/matrix")
    public ResponseEntity<List<EventAttendanceRowDTO>> getAttendanceMatrix(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        return ResponseEntity.ok(eventService.getEventAttendanceTable(organizationId, eventId));
    }
}
