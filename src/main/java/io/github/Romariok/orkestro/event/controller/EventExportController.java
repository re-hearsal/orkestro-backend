package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/events/exports")
@Tag(name = "Events - Export", description = "API для экспорта расписания событий")
public class EventExportController {

    private final EventService eventService;

    @Operation(
            summary = "Экспорт расписания в iCal",
            description = "Экспортирует расписание событий текущего пользователя в формате iCalendar (.ics)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Расписание экспортировано",
                    content = @Content(mediaType = "text/calendar", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = "/schedule.ics", produces = "text/calendar")
    public ResponseEntity<ByteArrayResource> exportScheduleAsIcs(
            @Parameter(description = "Включить отклоненные события", example = "false") @RequestParam(defaultValue = "false") boolean includeDeclined) {
        String ics = eventService.exportCurrentUserScheduleAsIcal(includeDeclined);
        ByteArrayResource resource = new ByteArrayResource(ics.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("schedule.ics").build().toString())
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(resource);
    }

    @Operation(
            summary = "Экспорт расписания в CSV",
            description = "Экспортирует расписание событий текущего пользователя в формате CSV."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Расписание экспортировано",
                    content = @Content(mediaType = "text/csv", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = "/schedule.csv", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> exportScheduleAsCsv(
            @Parameter(description = "Включить отклоненные события", example = "false") @RequestParam(defaultValue = "false") boolean includeDeclined) {
        String csv = eventService.exportCurrentUserScheduleAsCsv(includeDeclined);
        ByteArrayResource resource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("schedule.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
