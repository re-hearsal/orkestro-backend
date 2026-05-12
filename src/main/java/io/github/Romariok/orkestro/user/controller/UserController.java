package io.github.Romariok.orkestro.user.controller;

import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.dto.CurrentUserResponseDTO;
import io.github.Romariok.orkestro.user.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.user.dto.MusicalRoleUpdateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TelegramLinkTokenResponseDTO;
import io.github.Romariok.orkestro.user.dto.VkLinkTokenResponseDTO;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import io.github.Romariok.orkestro.user.service.UserVkLinkService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
@RequestMapping("/api/v1/users/me")
@Tag(name = "Users", description = "API для управления пользователями и их профилями")
public class UserController {

    private final SecurityUtils securityUtils;
    private final MusicalRoleService musicalRoleService;
    private final UserTelegramLinkService userTelegramLinkService;
    private final UserVkLinkService userVkLinkService;
    private final UserService userService;
    private final OrganizationUserService organizationUserService;

    @Operation(
            summary = "Получить профиль текущего пользователя",
            description = "Возвращает полную информацию о профиле аутентифицированного пользователя. " +
                    "Включает персональные данные, канал уведомлений, язык интерфейса и ссылку на изображение профиля."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Профиль пользователя успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CurrentUserResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"id\": 1, \"username\": \"john_doe\", \"name\": \"John Doe\", \"email\": \"john@example.com\", \"location\": \"Moscow\", \"birthDate\": \"1990-05-15\", \"notificationChannel\": \"EMAIL\", \"preferredLanguage\": \"RU\", \"profileImageFileId\": 123, \"telegramUserId\": null}"
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
                                    name = "Требуется авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Профиль не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/me\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<CurrentUserResponseDTO> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    @Operation(
            summary = "Получить музыкальные инструменты пользователя",
            description = "Возвращает список музыкальных инструментов, на которых играет текущий пользователь. " +
                    "Каждый элемент содержит ID инструмента и его название."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список инструментов успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MusicalRoleDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Несколько инструментов",
                                            value = "[{\"instrumentId\": 1, \"instrumentName\": \"Guitar\"}, {\"instrumentId\": 2, \"instrumentName\": \"Piano\"}]"
                                    ),
                                    @ExampleObject(
                                            name = "Пустой список",
                                            value = "[]"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/musical-roles")
    public ResponseEntity<List<MusicalRoleDTO>> getMyMusicalRoles() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(musicalRoleService.getUserMusicalRoles(currentUserId));
    }

    @Operation(
            summary = "Получить организации текущего пользователя",
            description = "Возвращает список организаций, в которых состоит текущий пользователь."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список организаций получен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationDTO>> getMyOrganizations() {
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(organizationUserService.getUserOrganizations(currentUserId));
    }

    @Operation(
            summary = "Установить музыкальные инструменты пользователя",
            description = "Полностью заменяет список музыкальных инструментов пользователя на новый. " +
                    "Все существующие связи будут удалены и заменены на указанные в запросе. " +
                    "Если передан пустой список - все инструменты будут удалены."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Инструменты успешно обновлены",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Нарушение бизнес-правил",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Инструмент не найден",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"Instrument not found: 999\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Несколько инструментов не найдено",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"One or more instruments not found for ids: [1, 999, 888]\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный ID инструмента",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"instrumentIds[0]: must be greater than 0\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": [\"instrumentIds[0]: must be greater than 0\"]}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/musical-roles\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/musical-roles")
    public ResponseEntity<Void> setMyInstruments(
            @Parameter(
                    description = "Список ID инструментов для установки",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MusicalRoleUpdateRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Установить инструменты",
                                            value = "{\"instrumentIds\": [1, 2, 3]}"
                                    ),
                                    @ExampleObject(
                                            name = "Очистить все инструменты",
                                            value = "{\"instrumentIds\": []}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody MusicalRoleUpdateRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        musicalRoleService.setUserInstruments(currentUserId, request.getInstrumentIds());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Добавить музыкальный инструмент",
            description = "Добавляет один музыкальный инструмент в список инструментов текущего пользователя. " +
                    "Если инструмент уже добавлен - ничего не происходит (идемпотентная операция)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Инструмент успешно добавлен",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Сущность не найдена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пользователь не найден",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/me/musical-roles/1\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Инструмент не найден",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"Instrument not found: 999\", \"path\": \"/api/v1/users/me/musical-roles/999\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный ID",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"instrumentId: must be greater than 0\", \"path\": \"/api/v1/users/me/musical-roles/-1\", \"details\": [\"instrumentId: must be greater than 0\"]}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/musical-roles/1\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/musical-roles/1\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/musical-roles/{instrumentId}")
    public ResponseEntity<Void> addMyInstrument(
            @Parameter(
                    description = "ID добавляемого инструмента",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", minimum = "1")
            )
            @PathVariable @Positive Long instrumentId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        musicalRoleService.addInstrumentToUser(currentUserId, instrumentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить музыкальный инструмент",
            description = "Удаляет один музыкальный инструмент из списка инструментов текущего пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Инструмент успешно удален",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный ID инструмента",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Некорректный ID",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"instrumentId: must be greater than 0\", \"path\": \"/api/v1/users/me/musical-roles/-1\", \"details\": [\"instrumentId: must be greater than 0\"]}"
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
                                    name = "Требуется авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/musical-roles/1\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/musical-roles/1\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/musical-roles/{instrumentId}")
    public ResponseEntity<Void> removeMyInstrument(
            @Parameter(
                    description = "ID удаляемого инструмента",
                    required = true,
                    example = "1",
                    schema = @Schema(type = "integer", minimum = "1")
            )
            @PathVariable @Positive Long instrumentId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        musicalRoleService.removeInstrumentFromUser(currentUserId, instrumentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Создать токен для привязки Telegram",
            description = "Генерирует одноразовый токен, который пользователь может использовать для привязки своего аккаунта Telegram. " +
                    "Токен отправляется пользователю через бот и используется для подтверждения владения аккаунтом. " +
                    "Внимание: напрямую установить Telegram как канал уведомлений нельзя - нужно использовать этот токен."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен успешно создан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TelegramLinkTokenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"token\": \"tlg_a1b2c3d4e5f6g7h8i9j0\"}"
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
                                    name = "Требуется авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/telegram/link-token\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Профиль не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/me/telegram/link-token\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/telegram/link-token\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/telegram/link-token")
    public ResponseEntity<TelegramLinkTokenResponseDTO> createTelegramLinkToken() {
        String token = userTelegramLinkService.createLinkTokenForCurrentUser();
        return ResponseEntity.ok(new TelegramLinkTokenResponseDTO(token));
    }

    @Operation(
            summary = "Отвязать аккаунт Telegram",
            description = "Отвязывает аккаунт Telegram от текущего пользователя. " +
                    "Канал уведомлений сбрасывается на EMAIL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Аккаунт успешно отвязан", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/telegram/link")
    public ResponseEntity<Void> unlinkTelegram() {
        Long currentUserId = securityUtils.getCurrentUserId();
        userTelegramLinkService.unlinkTelegram(currentUserId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить изображение профиля",
            description = "Загружает и устанавливает новое изображение профиля для текущего пользователя. " +
                    "Если у пользователя уже было изображение профиля, оно будет заменено. " +
                    "Старое изображение удаляется автоматически, если оно не используется другими сущностями. " +
                    "Поддерживаемые форматы: JPEG, PNG, GIF, WEBP. Максимальный размер: 30MB."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Изображение профиля успешно обновлено",
                    content = @Content
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
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Profile image must be a non-empty image file\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Не изображение",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Profile image must be an image file\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Файл слишком большой",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 413, \"error\": \"Validation failed\", \"message\": \"Uploaded file is too large (max 30MB)\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Профиль не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 415, \"error\": \"Unsupported media type\", \"message\": \"Content type is not supported\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCurrentUserProfileImage(
            @Parameter(
                    description = "Изображение профиля (JPEG, PNG, GIF, WEBP)",
                    required = true,
                    schema = @Schema(type = "string", format = "binary")
            )
            @RequestParam MultipartFile file) {
        userService.updateCurrentUserProfileImage(file);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить изображение профиля",
            description = "Удаляет текущее изображение профиля пользователя. " +
                    "Если изображение профиля не установлено - ничего не происходит."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Изображение профиля успешно удалено",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется авторизация",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Профиль не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/users/me/profile-image\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/profile-image")
    public ResponseEntity<Void> deleteCurrentUserProfileImage() {
        userService.deleteCurrentUserProfileImage();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Создать токен для привязки ВКонтакте",
            description = "Генерирует одноразовый токен для привязки аккаунта ВКонтакте. " +
                    "Токен передаётся боту ВКонтакте, который отправляет его в очередь RabbitMQ."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Токен успешно создан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VkLinkTokenResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"token\": \"vk_token_example\"}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/vk/link-token")
    public ResponseEntity<VkLinkTokenResponseDTO> createVkLinkToken() {
        String token = userVkLinkService.generateLinkTokenForCurrentUser();
        return ResponseEntity.ok(new VkLinkTokenResponseDTO(token));
    }

    @Operation(
            summary = "Отвязать аккаунт ВКонтакте",
            description = "Отвязывает аккаунт ВКонтакте от текущего пользователя. " +
                    "Канал уведомлений сбрасывается на EMAIL."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Аккаунт успешно отвязан", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/vk/link")
    public ResponseEntity<Void> unlinkVk() {
        userVkLinkService.unlinkVk(securityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
