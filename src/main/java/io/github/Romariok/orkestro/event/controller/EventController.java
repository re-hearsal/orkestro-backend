package io.github.Romariok.orkestro.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventDuplicateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;

import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
public class EventController {
   private final EventService eventService;

   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<EventDTO> createEvent(
         @PathVariable @Positive Long organizationId,
         @Valid @ModelAttribute EventCreateRequestDTO request) {
      EventDTO created = eventService.createEventInOrganization(organizationId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }

   @PostMapping("/{eventId}/duplicates")
   public ResponseEntity<java.util.List<EventDTO>> duplicateEvent(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long eventId,
         @Valid @org.springframework.web.bind.annotation.RequestBody EventDuplicateRequestDTO request) {
      return ResponseEntity.status(HttpStatus.CREATED)
            .body(eventService.duplicateEvent(organizationId, eventId, request.getStartTimes()));
   }

   @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<EventDTO> updateEvent(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long eventId,
         @Valid @ModelAttribute EventUpdateRequestDTO request) {
      return ResponseEntity.ok(eventService.updateEvent(organizationId, eventId, request));
   }

   @DeleteMapping("/{eventId}")
   public ResponseEntity<Void> deleteEvent(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long eventId) {
      eventService.deleteEvent(organizationId, eventId);
      return ResponseEntity.noContent().build();
   }
}
