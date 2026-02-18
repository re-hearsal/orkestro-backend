package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventSearchRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
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
@RequestMapping("/api/v1/organizations/{organizationId}/events")
@Tag(name = "Events - Query", description = "API для получения информации о событиях")
public class EventQueryController {

    private final EventService eventService;

    @Operation(
            summary = "Получить событие",
            description = "Возвращает информацию о конкретном событии по ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Событие найдено", content = @Content(schema = @Schema(implementation = EventDTO.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDTO> getEvent(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID события", required = true) @PathVariable @Positive Long eventId) {
        return ResponseEntity.ok(eventService.getEventForCurrentUser(organizationId, eventId));
    }

    @Operation(
            summary = "Поиск событий",
            description = "Возвращает страницу событий с возможностью фильтрации по различным параметрам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список событий получен", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/search/page")
    public ResponseEntity<Page<EventDTO>> searchEventsPage(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "Параметры поиска") @ModelAttribute EventSearchRequestDTO request,
            @Parameter(description = "Параметры пагинации") @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.searchEventsPageForCurrentUserInOrganization(organizationId, request, pageable));
    }
}
