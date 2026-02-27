package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.dto.EventCommentCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventPageDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/events")
@Tag(name = "Events - Comments", description = "API для комментариев к мероприятиям")
public class EventCommentController {

    private final EventService eventService;

    @Operation(
            summary = "Добавить комментарий к мероприятию",
            description = "Создает комментарий к мероприятию. Комментарий может оставить создатель мероприятия или пользователь с правом EVENT_WRITE_COMMENT."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Комментарий создан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventCommentDTO.class),
                            examples = @ExampleObject(
                                    value = "{\"id\":5,\"authorUserId\":42,\"authorName\":\"Иван Петров\",\"text\":\"Прогресс есть, нужно доработать духовые.\",\"createdAt\":\"2026-02-27T10:15:30Z\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Событие не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/{eventId}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EventCommentDTO> createEventComment(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID мероприятия", required = true) @PathVariable @Positive Long eventId,
            @Parameter(
                    description = "Текст комментария",
                    required = true,
                    content = @Content(examples = @ExampleObject(
                            value = "{\"text\":\"На этой неделе прогресс по ритму заметно улучшился.\"}"
                    ))
            )
            @Valid @RequestBody EventCommentCreateRequestDTO request) {
        EventCommentDTO created = eventService.createEventComment(organizationId, eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Получить комментарии по списку мероприятий",
            description = "Возвращает страницу мероприятий (пагинация по eventIds). Для каждого мероприятия возвращаются количество комментариев и полный список комментариев."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Комментарии получены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EventCommentsByEventPageDTO.class),
                            examples = @ExampleObject(
                                    value = "{\"page\":0,\"size\":2,\"totalElements\":3,\"totalPages\":2,\"first\":true,\"last\":false,\"content\":[{\"eventId\":11,\"commentsCount\":2,\"comments\":[{\"id\":1,\"authorUserId\":42,\"authorName\":\"Иван Петров\",\"text\":\"Первые итоги хорошие\",\"createdAt\":\"2026-02-27T10:00:00Z\"}]},{\"eventId\":12,\"commentsCount\":0,\"comments\":[]}]}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации параметров", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Одно или несколько мероприятий не найдено", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/comments")
    public ResponseEntity<EventCommentsByEventPageDTO> getEventComments(
            @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID мероприятий для выборки. Пагинация применяется к этому списку.", required = true)
            @RequestParam @Size(min = 1) List<@Positive Long> eventIds,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventCommentsByEventIds(organizationId, eventIds, pageable));
    }
}
