package io.github.Romariok.orkestro.task.controller;

import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.dto.TaskFileAttachRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskVisibilityUpdateRequestDTO;
import io.github.Romariok.orkestro.task.service.TaskService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/tasks")
@Tag(name = "Tasks", description = "API для управления задачами в организации")
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Создать задачу",
            description = "Создает новую задачу в организации. Задача может быть назначена конкретному пользователю. " +
                    "Поддерживается видимость: ALL_MEMBERS (доступна всем участникам), ROLE_RESTRICTED (только назначенном определенным ролям). " +
                    "Можно прикрепить до 50 файлов при создании."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Задача успешно создана",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"id\": 1, \"title\": \"Prepare concert\", \"description\": \"Prepare equipment for upcoming concert\", \"visibility\": \"OPEN\", \"assigneeUserId\": 5, \"assigneeName\": \"John Doe\", \"organizationId\": 1, \"createdAt\": \"2026-02-18T10:00:00\", \"updatedAt\": \"2026-02-18T10:00:00\", \"fileIds\": [1, 2], \"closedAt\": null}"
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
                                            name = "Название слишком длинное",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"title: size must be between 0 and 255\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": [\"title: size must be between 0 and 255\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Превышен лимит файлов",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Task files limit reached (50)\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Файл пустой",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"Files must be non-empty with valid names\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Неверный ID исполнителя",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"assigneeUserId: must be greater than 0\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": [\"assigneeUserId: must be greater than 0\"]}"
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
                                    name = "Требуется авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Доступ запрещен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Нет доступа к организации",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 403, \"error\": \"Access denied\", \"message\": \"Access denied\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Организация или пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Организация не найдена",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"Organization not found: 999\", \"path\": \"/api/v1/organizations/999/tasks\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Пользователь-исполнитель не найден",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                                    )
                            }
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/organizations/1/tasks\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskDTO> createTask(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(
                    description = "Данные для создания задачи",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = TaskCreateRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Открытая задача без файлов",
                                            value = "{\"title\": \"Prepare concert\", \"description\": \"Prepare equipment for upcoming concert\", \"visibility\": \"OPEN\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Задача с исполнителем",
                                            value = "{\"title\": \"Sound check\", \"description\": \"Do sound check before event\", \"assigneeUserId\": 5, \"visibility\": \"ASSIGNED\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Задача с файлами",
                                            value = "{\"title\": \"Upload documents\", \"visibility\": \"OPEN\", \"files\": [\"(binary)\", \"(binary)\"]}"
                                    )
                            }
                    )
            )
            @Valid @ModelAttribute TaskCreateRequestDTO request) {
        TaskDTO created = taskService.createTaskInOrganization(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Обновить видимость задачи",
            description = "Изменяет видимость задачи. Возможные значения: ALL_MEMBERS, ROLE_RESTRICTED."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Видимость успешно обновлена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Некорректная видимость",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"visibility: must not be null\", \"path\": \"/api/v1/organizations/1/tasks/1/visibility\", \"details\": [\"visibility: must not be null\"]}"
                            )
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
                    description = "Задача не найдена",
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
    @PutMapping("/{taskId}/visibility")
    public ResponseEntity<TaskDTO> updateTaskVisibility(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID задачи", required = true, example = "1")
            @PathVariable @Positive Long taskId,
            @Parameter(
                    description = "Новая видимость",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Изменить на ALL_MEMBERS",
                                    value = "{\"visibility\": \"ALL_MEMBERS\"}"
                            )
                    )
            )
            @Valid @RequestBody TaskVisibilityUpdateRequestDTO request) {
        return ResponseEntity.ok(taskService.updateTaskVisibility(organizationId, taskId, request));
    }

    @Operation(
            summary = "Получить закрытые задачи",
            description = "Возвращает страницу закрытых задач, доступных текущему пользователю в организации."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список задач получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
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
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/closed/page")
    public ResponseEntity<Page<TaskDTO>> getClosedTasksPage(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "Параметры пагинации")
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getClosedTasksForCurrentUser(organizationId, pageable));
    }

    @Operation(
            summary = "Прикрепить файл к задаче",
            description = "Прикрепляет файл к существующей задаче. Максимум 50 файлов на задачу."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Файл успешно прикреплен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Лимит файлов",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Превышен лимит файлов",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Task files limit reached (50)\", \"path\": \"/api/v1/organizations/1/tasks/1/files\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Пустой файл",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"File is required\", \"path\": \"/api/v1/organizations/1/tasks/1/files\", \"details\": []}"
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
                    description = "Задача не найдена",
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
    @PostMapping(value = "/{taskId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskDTO> attachFileToTask(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID задачи", required = true, example = "1")
            @PathVariable @Positive Long taskId,
            @Parameter(
                    description = "Файл для прикрепления",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            examples = @ExampleObject(
                                    name = "Пример файла",
                                    value = "{\"file\": \"(binary)\"}"
                            )
                    )
            )
            @Valid @ModelAttribute TaskFileAttachRequestDTO request) {
        TaskDTO updated = taskService.attachFileToTaskForCurrentUser(organizationId, taskId, request.getFile());
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @Operation(
            summary = "Удалить файл из задачи",
            description = "Удаляет прикрепленный файл из задачи."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Файл успешно удален",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskDTO.class)
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
                    description = "Задача или файл не найдены",
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
    @DeleteMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<TaskDTO> deleteFileFromTask(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "ID задачи", required = true, example = "1")
            @PathVariable @Positive Long taskId,
            @Parameter(description = "ID файла", required = true, example = "1")
            @PathVariable @Positive Long fileId) {
        return ResponseEntity.ok(taskService.deleteTaskFileForCurrentUser(organizationId, taskId, fileId));
    }

    @Operation(
            summary = "Получить доступные задачи",
            description = "Возвращает страницу задач, доступных текущему пользователю."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список задач получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class)
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
                    responseCode = "500",
                    description = "Внутренняя ошибка сервера",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/available/page")
    public ResponseEntity<Page<TaskDTO>> getAvailableTasksPage(
            @Parameter(description = "ID организации", required = true, example = "1")
            @PathVariable @Positive Long organizationId,
            @Parameter(description = "Параметры пагинации")
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getAvailableTasksForCurrentUser(organizationId, pageable));
    }
}
