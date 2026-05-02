package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.dto.SectionUpdateRequestDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
@RequestMapping("/api/v1/sections")
@Tag(name = "Sections", description = "API для управления секциями (группами внутри организации)")
public class SectionController {

        private final SectionService sectionService;

        @Operation(summary = "Создать вложенную секцию", description = "Создает новую секцию внутри существующей секции. Секции образуют иерархию - родительская секция может содержать дочерние.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Секция успешно создана", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SectionDTO.class), examples = @ExampleObject(name = "Пример ответа", value = "{\"id\": 2, \"name\": \"Rock Band\", \"description\": \"Rock music group\", \"parentSectionId\": 1, \"organizationId\": 1}"))),
                        @ApiResponse(responseCode = "400", description = "Ошибка валидации / Нарушение бизнес-правил", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {
                                        @ExampleObject(name = "Имя секции пустое", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"name: must not be blank\", \"path\": \"/api/v1/sections/1/sections\", \"details\": [\"name: must not be blank\"]}"),
                                        @ExampleObject(name = "Секция с таким именем уже существует", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Section with name 'Rock Band' already exists in parent section\", \"path\": \"/api/v1/sections/1/sections\", \"details\": []}")
                        })),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "Требуется авторизация", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/sections/1/sections\", \"details\": []}"))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "Нет прав на создание", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 403, \"error\": \"Access denied\", \"message\": \"Access denied\", \"path\": \"/api/v1/sections/1/sections\", \"details\": []}"))),
                        @ApiResponse(responseCode = "404", description = "Родительская секция не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "Секция не найдена", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"Section not found: 999\", \"path\": \"/api/v1/sections/999/sections\", \"details\": []}"))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "Ошибка сервера", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/sections/1/sections\", \"details\": []}")))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping("/{parentSectionId}/sections")
        public ResponseEntity<SectionDTO> createSectionInSection(
                        @Parameter(description = "ID родительской секции", required = true, example = "1") @PathVariable @Positive Long parentSectionId,
                        @Parameter(description = "Данные для создания секции", required = true, content = @Content(examples = {
                                        @ExampleObject(name = "Создать секцию", value = "{\"name\": \"Jazz Ensemble\", \"description\": \"Jazz music group\"}")
                        })) @Valid @RequestBody SectionCreateRequestDTO request) {
                SectionDTO created = sectionService.createSectionInSection(parentSectionId, request);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        @Operation(summary = "Получить секцию по ID", description = "Возвращает данные секции. Доступно принятым участникам организации, в которой создана секция.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Секция найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SectionDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping("/{sectionId}")
        public ResponseEntity<SectionDTO> getSectionById(
                        @Parameter(description = "ID секции", required = true, example = "1") @PathVariable @Positive Long sectionId) {
                return ResponseEntity.ok(sectionService.getSectionById(sectionId));
        }

        @Operation(summary = "Получить дочерние секции", description = "Возвращает список дочерних секций для указанной секции. Доступно принятым участникам организации, в которой создана секция.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список дочерних секций получен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = SectionDTO.class)))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping("/{parentSectionId}/sections")
        public ResponseEntity<List<SectionDTO>> getChildSections(
                        @Parameter(description = "ID родительской секции", required = true, example = "1") @PathVariable @Positive Long parentSectionId) {
                return ResponseEntity.ok(sectionService.getChildSections(parentSectionId));
        }

        @Operation(summary = "Обновить секцию", description = "Частично обновляет секцию. Обновляются только переданные поля.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Секция обновлена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SectionDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PatchMapping("/{sectionId}")
        public ResponseEntity<SectionDTO> updateSection(
                        @Parameter(description = "ID секции", required = true, example = "1") @PathVariable @Positive Long sectionId,
                        @Parameter(description = "Данные для обновления секции", required = true) @RequestBody SectionUpdateRequestDTO request) {
                return ResponseEntity.ok(sectionService.updateSection(sectionId, request));
        }

        @Operation(summary = "Удалить секцию", description = "Удаляет секцию. Удаление возможно только если в секции нет участников.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Секция успешно удалена", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Нарушение бизнес-правил - в секции есть участники", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = @ExampleObject(name = "Секция не пуста", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Cannot delete section with members\", \"path\": \"/api/v1/sections/1\", \"details\": []}"))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @DeleteMapping("/{sectionId}")
        public ResponseEntity<Void> deleteSection(
                        @Parameter(description = "ID секции", required = true, example = "1") @PathVariable @Positive Long sectionId) {
                sectionService.deleteSection(sectionId);
                return ResponseEntity.noContent().build();
        }

}
