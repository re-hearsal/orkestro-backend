package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.service.EventService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/exports")
public class EventExportController {

    private final EventService eventService;

    @GetMapping(value = "/schedule.ics", produces = "text/calendar")
    public ResponseEntity<ByteArrayResource> exportScheduleAsIcs(
            @RequestParam(defaultValue = "false") boolean includeDeclined) {
        String ics = eventService.exportCurrentUserScheduleAsIcal(includeDeclined);
        ByteArrayResource resource = new ByteArrayResource(ics.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("schedule.ics").build().toString())
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(resource);
    }

    @GetMapping(value = "/schedule.csv", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> exportScheduleAsCsv(
            @RequestParam(defaultValue = "false") boolean includeDeclined) {
        String csv = eventService.exportCurrentUserScheduleAsCsv(includeDeclined);
        ByteArrayResource resource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("schedule.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
