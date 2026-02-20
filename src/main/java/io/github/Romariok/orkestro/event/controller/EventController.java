package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventDuplicateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Events", description = "API для управления событиями (концертами, репетициями)")
public class EventController {

    private final EventService eventService;

    @Operation(
            summary = "Создать событие",
            description = "Создает новое событие (концерт, репетицию и т.д.) в организации. " +
                    "Событие имеет название, описание, время начала и окончания, место проведения."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Событие успешно создано",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"id\":1,\"title\":\"Concert\",\"description\":\"New Year concert\",\"eventType\":\"CONCERT\",\"startTime\":\"2026-12-31T19:00:00Z\",\"endTime\":\"2026-12-31T23:00:00Z\",\"location\":\"Moscow Hall\",\"organizationId\":1,\"participantUserIds\":[101,102],\"participantSectionIds\":[10,11],\"includeAllOrganizationMembers\":false,\"fileIds\":[],\"songIds\":[],\"createdAt\":\"2026-02-18T10:00:00Z\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Время окончания раньше начала",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"End time must be after start time\", \"path\": \"/api/v1/organizations/1/events\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Превышен лимит файлов",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Event files limit reached (50)\", \"path\": \"/api/v1/organizations/1/events\", \"details\": []}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Организация не найдена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDTO> createEvent(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(
                    description = "Данные для создания события",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = EventCreateRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Создать концерт",
                                            value = "{\"title\":\"New Year Concert\",\"description\":\"Annual New Year concert\",\"eventType\":\"CONCERT\",\"startTime\":\"2026-12-31T19:00:00Z\",\"endTime\":\"2026-12-31T23:00:00Z\",\"location\":\"Moscow Concert Hall\",\"participantSectionIds\":[10,11],\"includeAllOrganizationMembers\":false}"
                                    )
                            }
                    )
            )
            @Valid @ModelAttribute EventCreateRequestDTO request) {
        EventDTO created = eventService.createEventInOrganization(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Дублировать событие",
            description = "Создает копии существующего события с новыми временами начала."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "События успешно созданы",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Событие не найдено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{eventId}/duplicates")
    public ResponseEntity<java.util.List<EventDTO>> duplicateEvent(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true)
            @PathVariable @Positive Long eventId,
            @Parameter(
                    description = "Время начала для копий",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Дублировать на несколько дат",
                                    value = "{\"startTimes\": [\"2027-01-15T19:00:00\", \"2027-02-15T19:00:00\", \"2027-03-15T19:00:00\"]}"
                            )
                    )
            )
            @Valid @org.springframework.web.bind.annotation.RequestBody EventDuplicateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.duplicateEvent(organizationId, eventId, request.getStartTimes()));
    }

    @Operation(
            summary = "Обновить событие",
            description = "Обновляет информацию о событии."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Событие обновлено", content = @Content(schema = @Schema(implementation = EventDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDTO> updateEvent(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true)
            @PathVariable @Positive Long eventId,
            @Valid @ModelAttribute EventUpdateRequestDTO request) {
        return ResponseEntity.ok(eventService.updateEvent(organizationId, eventId, request));
    }

    @Operation(
            summary = "Удалить событие",
            description = "Удаляет событие из организации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Событие удалено", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true)
            @PathVariable @Positive Long eventId) {
        eventService.deleteEvent(organizationId, eventId);
        return ResponseEntity.noContent().build();
    }
}
