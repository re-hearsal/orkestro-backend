package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventAttendanceMarkRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
@Tag(name = "Events - Attendance", description = "API для управления посещаемостью событий")
public class EventAttendanceController {

    private final EventService eventService;

    @Operation(
            summary = "Отметить посещаемость",
            description = "Отмечает статус посещения участником события (присутствовал, отсутствовал и т.д.)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Посещаемость отмечена", content = @Content),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие или пользователь не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{eventId}/attendance")
    public ResponseEntity<Void> markEventAttendance(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId,
            @Parameter(description = "Данные о посещаемости", required = true, content = @Content(examples = {
                @ExampleObject(name = "Отметить присутствие", value = "{\"participantUserId\": 5, \"attendanceStatus\": \"PRESENT\"}"),
                @ExampleObject(name = "Отметить отсутствие", value = "{\"participantUserId\": 5, \"attendanceStatus\": \"ABSENT\"}")
            }))
            @Valid @RequestBody EventAttendanceMarkRequestDTO request) {
        eventService.markEventAttendance(organizationId, eventId, request.getParticipantUserId(), request.getAttendanceStatus());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить матрицу посещаемости",
            description = "Возвращает таблицу посещаемости для всех участников события."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Матрица получена", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{eventId}/attendance/matrix")
    public ResponseEntity<List<EventAttendanceRowDTO>> getAttendanceMatrix(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId) {
        return ResponseEntity.ok(eventService.getEventAttendanceTable(organizationId, eventId));
    }

    @Operation(
            summary = "Выгрузить матрицу посещаемости в CSV",
            description = "Возвращает CSV-файл с матрицей посещаемости для всех участников события."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV-файл сформирован", content = @Content(mediaType = "text/csv", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping(value = "/{eventId}/attendance/matrix.csv", produces = "text/csv")
    public ResponseEntity<ByteArrayResource> exportAttendanceMatrixCsv(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId) {
        String csv = eventService.exportEventAttendanceMatrixAsCsv(organizationId, eventId);
        ByteArrayResource resource = new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("event-attendance-matrix.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}
