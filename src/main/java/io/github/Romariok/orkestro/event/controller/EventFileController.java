package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventFileAttachRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
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
@Tag(name = "Events - Files", description = "API для управления файлами событий")
public class EventFileController {

    private final EventService eventService;

    @Operation(
            summary = "Прикрепить файл к событию",
            description = "Добавляет файл (афиша, документы и т.д.) к событию. Максимум 50 файлов на событие."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Файл прикреплен", content = @Content(schema = @Schema(implementation = EventDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации / Лимит файлов", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/{eventId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EventDTO> addFileToEvent(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId,
            @Parameter(description = "Файл для прикрепления", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, examples = {
                @ExampleObject(name = "Пример файла", value = "{\"file\": \"(binary)\"}")
            }))
            @Valid @ModelAttribute EventFileAttachRequestDTO request) {
        EventDTO updated = eventService.attachFileToEvent(organizationId, eventId, request.getFile());
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @Operation(
            summary = "Удалить файл из события",
            description = "Удаляет прикрепленный файл из события."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл удален", content = @Content(schema = @Schema(implementation = EventDTO.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие или файл не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{eventId}/files/{fileId}")
    public ResponseEntity<EventDTO> deleteFileFromEvent(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId,
            @Parameter(description = "ID файла", required = true) @PathVariable @Positive Long fileId) {
        return ResponseEntity.ok(eventService.deleteEventFile(organizationId, eventId, fileId));
    }
}
