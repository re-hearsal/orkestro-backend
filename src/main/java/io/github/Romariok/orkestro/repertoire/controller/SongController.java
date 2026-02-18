package io.github.Romariok.orkestro.repertoire.controller;

import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongFileUploadRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.repertoire.service.RepertoireService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/v1/organizations/{organizationId}/repertoire/songs")
@Tag(name = "Repertoire", description = "API для управления репертуаром (песнями)")
public class SongController {

    private final RepertoireService repertoireService;

    @Operation(
            summary = "Создать песню",
            description = "Добавляет новую песню в репертуар организации."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Песня успешно создана",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SongDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"id\": 1, \"title\": \"Bohemian Rhapsody\", \"artist\": \"Queen\", \"genre\": \"Rock\", \"bpm\": 72, \"durationSeconds\": 354, \"key\": \"Bb Major\", \"organizationId\": 1, \"fileId\": null}"
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
                                            name = "Название песни пустое",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"title: must not be blank\", \"path\": \"/api/v1/organizations/1/repertoire/songs\", \"details\": [\"title: must not be blank\"]}"
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
    public ResponseEntity<SongDTO> createSong(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(
                    description = "Данные для создания песни",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = SongCreateRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Создать песню",
                                            value = "{\"title\": \"Bohemian Rhapsody\", \"artist\": \"Queen\", \"genre\": \"Rock\", \"bpm\": 72, \"durationSeconds\": 354, \"key\": \"Bb Major\"}"
                                    )
                            }
                    )
            )
            @Valid @ModelAttribute SongCreateRequestDTO request) {
        SongDTO created = repertoireService.createSong(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Загрузить и прикрепить файл к песне",
            description = "Загружает файл (аудио, ноты, лирика) и прикрепляет его к песне."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Файл успешно прикреплен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SongDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Файл слишком большой",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Файл слишком большой",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 413, \"error\": \"Validation failed\", \"message\": \"Uploaded file is too large (max 30MB)\", \"path\": \"/api/v1/organizations/1/repertoire/songs/1/files\", \"details\": []}"
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
                    description = "Песня не найдена",
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
    @PostMapping(value = "/{songId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongDTO> uploadAndAttachFile(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID песни", required = true)
            @PathVariable @Positive Long songId,
            @Parameter(
                    description = "Файл для загрузки",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            examples = @ExampleObject(
                                    name = "Аудио файл",
                                    value = "{\"file\": \"(binary)\", \"fileType\": \"AUDIO\"}"
                            )
                    )
            )
            @Valid @ModelAttribute SongFileUploadRequestDTO request) {
        SongDTO updated = repertoireService.uploadAndAttachSongFile(organizationId, songId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @Operation(
            summary = "Обновить песню",
            description = "Обновляет информацию о песне в репертуаре."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Песня обновлена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SongDTO.class)
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
                    description = "Песня не найдена",
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
    @PutMapping("/{songId}")
    public ResponseEntity<SongDTO> updateSong(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID песни", required = true)
            @PathVariable @Positive Long songId,
            @Parameter(
                    description = "Данные для обновления",
                    required = true,
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Обновить BPM",
                                            value = "{\"bpm\": 80}"
                                    ),
                                    @ExampleObject(
                                            name = "Обновить несколько полей",
                                            value = "{\"title\": \"New Title\", \"artist\": \"New Artist\", \"bpm\": 100}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody SongUpdateRequestDTO request) {
        return ResponseEntity.ok(repertoireService.updateSong(organizationId, songId, request));
    }

    @Operation(
            summary = "Удалить песню",
            description = "Удаляет песню из репертуара организации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Песня удалена", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Песня не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> deleteSong(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID песни", required = true)
            @PathVariable @Positive Long songId) {
        repertoireService.deleteSong(organizationId, songId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Получить песню",
            description = "Возвращает информацию о конкретной песне."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Песня найдена", content = @Content(schema = @Schema(implementation = SongDTO.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Песня не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{songId}")
    public ResponseEntity<SongDTO> getSong(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID песни", required = true)
            @PathVariable @Positive Long songId) {
        return ResponseEntity.ok(repertoireService.getSong(organizationId, songId));
    }

    @Operation(
            summary = "Поиск песен (с пагинацией)",
            description = "Возвращает страницу песен репертуара организации с возможностью поиска по названию или исполнителю."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список песен получен", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/page")
    public ResponseEntity<Page<SongDTO>> searchSongsPage(
            @Parameter(description = "ID организации", required = true)
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "Поисковый запрос (название или исполнитель)")
            @RequestParam(required = false) String query,
            @Parameter(description = "Параметры пагинации")
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(repertoireService.searchSongs(organizationId, query, pageable));
    }
}
