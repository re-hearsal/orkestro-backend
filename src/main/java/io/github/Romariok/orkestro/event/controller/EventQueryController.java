package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventSearchRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
public class EventQueryController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEvent(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long eventId) {
        return ResponseEntity.ok(eventService.getEventForCurrentUser(organizationId, eventId));
    }

    @GetMapping("/search/page")
    public ResponseEntity<Page<EventDTO>> searchEventsPage(
            @PathVariable @Positive Long organizationId,
            @ModelAttribute EventSearchRequestDTO request,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.searchEventsPageForCurrentUserInOrganization(
                organizationId,
                request,
                pageable));
    }
}
