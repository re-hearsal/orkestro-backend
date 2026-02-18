package io.github.Romariok.orkestro.utils.file.controller;

import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.dto.FileUploadRequestDTO;
import io.github.Romariok.orkestro.utils.file.dto.FileUploadResponseDTO;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "API для управления файлами. Gonna be deprecated eventually")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FileReferenceService fileReferenceService;

    @Operation(
            summary = "Загрузить файл",
            description = "Загружает файл в хранилище. " +
                    "Если параметр uploadedByUserId не указан, файл загружается от имени текущего аутентифицированного пользователя. " +
                    "Поддерживаемые типы файлов: PDF, PHOTO, AUDIO, VIDEO, OTHER. " +
                    "Максимальный размер файла: 30MB."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Файл успешно загружен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileUploadResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Успешный ответ",
                                    value = "{\"id\": 1, \"name\": \"document.pdf\", \"fileType\": \"PDF\", \"size\": 1024}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Нарушение бизнес-правил",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Файл отсутствует или пустой",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"File is required\", \"path\": \"/api/v1/files/upload\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Имя файла отсутствует",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"File name is required\", \"path\": \"/api/v1/files/upload\", \"details\": []}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Необходима авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/files/upload\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Не поддерживаемый тип контента",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неверный Content-Type",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 415, \"error\": \"Unsupported media type\", \"message\": \"Content-Type 'text/plain' is not supported. Use 'multipart/form-data'\", \"path\": \"/api/v1/files/upload\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ошибка сервера",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/files/upload\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponseDTO> upload(
            @Parameter(
                    description = "Данные загружаемого файла",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = FileUploadRequestDTO.class)
                    )
            )
            @Valid @ModelAttribute FileUploadRequestDTO request) {
        if (request.getUploadedByUserId() == null && !isAuthenticated()) {
            throw new io.github.Romariok.orkestro.utils.exception.BusinessException("Authentication required when uploadedByUserId is not provided");
        }
        StoredFile storedFile = request.getUploadedByUserId() != null
                    ? fileStorageService.upload(
                          request.getFile(),
                          request.getFileType(),
                          request.getUploadedByUserId())
                    : fileStorageService.uploadForCurrentUser(
                          request.getFile(),
                          request.getFileType());
        FileUploadResponseDTO response = new FileUploadResponseDTO(
                    storedFile.getId(),
                    storedFile.getName(),
                    storedFile.getFileType(),
                    storedFile.getSize());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Удалить файл",
            description = "Удаляет файл из хранилища по его ID. " +
                    "Файл можно удалить только если он не привязан к каким-либо сущностям (событиям, задачам и т.д.). " +
                    "Перед удалением проверяется наличие ссылок на файл."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Файл успешно удален",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Нарушение бизнес-правил - файл привязан к сущностям",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Файл привязан к сущностям",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Cannot delete file that is still attached to entities\", \"path\": \"/api/v1/files/1\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Необходима авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/files/1\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Файл не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Файл не существует",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"File not found with id: 1\", \"path\": \"/api/v1/files/1\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Ошибка сервера",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/files/1\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(
            @Parameter(
                    description = "ID файла для удаления",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", minimum = "1")
            )
            @PathVariable @Positive Long fileId) {
        if (fileReferenceService.isFileReferenced(fileId)) {
            throw new io.github.Romariok.orkestro.utils.exception.BusinessException("Cannot delete file that is still attached to entities");
        }
        fileStorageService.delete(fileId);
        return ResponseEntity.noContent().build();
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
              && authentication.isAuthenticated()
              && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
