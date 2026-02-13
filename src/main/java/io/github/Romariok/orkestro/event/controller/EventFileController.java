package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventFileAttachRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
public class EventFileController {

    private final EventService eventService;

    @PostMapping(value = "/{eventId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDTO> addFileToEvent(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @Valid @ModelAttribute EventFileAttachRequestDTO request) {
        EventDTO updated = eventService.attachFileToEvent(organizationId, eventId, request.getFile());
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @DeleteMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<EventDTO> deleteFileFromEvent(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId,
            @PathVariable @Positive Long fileId) {
        return ResponseEntity.ok(eventService.deleteEventFile(organizationId, eventId, fileId));
    }
}
