package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventDescriptionTemplateService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/event-templates")
@Tag(name = "Events - Description Templates", description = "API для управления шаблонами описаний мероприятий")
public class EventDescriptionTemplateController {

    private final EventDescriptionTemplateService templateService;

    @Operation(
            summary = "Создать шаблон описания мероприятия",
            description = "Создает новый шаблон описания для мероприятий заданного типа. Требует права EVENT_MANAGE_DESCRIPTIONS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Шаблон создан",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventDescriptionTemplateDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDescriptionTemplateDTO> createTemplate(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Valid @RequestBody EventDescriptionTemplateCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.createTemplate(organizationId, request));
    }

    @Operation(
            summary = "Получить шаблоны описаний мероприятий",
            description = "Возвращает список шаблонов описаний. Можно фильтровать по типу мероприятия. Требует права EVENT_MANAGE_DESCRIPTIONS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список шаблонов получен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<List<EventDescriptionTemplateDTO>> listTemplates(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "Фильтр по типу мероприятия (REHEARSAL, CONCERT, OTHER)") @RequestParam(required = false) EventType eventType) {
        return ResponseEntity.ok(templateService.listTemplates(organizationId, eventType));
    }

    @Operation(
            summary = "Обновить шаблон описания мероприятия",
            description = "Обновляет существующий шаблон описания. Требует права EVENT_MANAGE_DESCRIPTIONS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Шаблон обновлен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventDescriptionTemplateDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Шаблон или организация не найдены",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping(value = "/{templateId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventDescriptionTemplateDTO> updateTemplate(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID шаблона", required = true) @PathVariable @Positive Long templateId,
            @Valid @RequestBody EventDescriptionTemplateCreateRequestDTO request) {
        return ResponseEntity.ok(templateService.updateTemplate(organizationId, templateId, request));
    }

    @Operation(
            summary = "Удалить шаблон описания мероприятия",
            description = "Удаляет шаблон описания. Требует права EVENT_MANAGE_DESCRIPTIONS."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Шаблон удален"),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Шаблон или организация не найдены",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID шаблона", required = true) @PathVariable @Positive Long templateId) {
        templateService.deleteTemplate(organizationId, templateId);
        return ResponseEntity.noContent().build();
    }
}
