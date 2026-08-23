package io.github.Romariok.orkestro.user.controller;

import io.github.Romariok.orkestro.user.dto.AuthResponseDTO;
import io.github.Romariok.orkestro.user.dto.LoginRequestDTO;
import io.github.Romariok.orkestro.user.dto.PasswordResetRequestDTO;
import io.github.Romariok.orkestro.user.dto.RegisterRequestDTO;
import io.github.Romariok.orkestro.user.dto.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.user.service.AuthService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API для регистрации, входа и управления аккаунтом")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает новый аккаунт пользователя. После успешной регистрации автоматически выполняет вход и возвращает JWT токен. " +
                    "Пароль должен содержать минимум 8 символов. Email должен быть уникальным."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Успешная регистрация",
                                    value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"type\": \"Bearer\", \"expiresIn\": 86400}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации / Имя пользователя или email уже заняты",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Имя пользователя занято",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Username is already taken\", \"path\": \"/api/v1/auth/register\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Email занят",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Business rule violation\", \"message\": \"Email is already in use\", \"path\": \"/api/v1/auth/register\", \"details\": []}"
                                    ),
                                    @ExampleObject(
                                            name = "Пароль слишком короткий",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"password: size must be between 8 and 100\", \"path\": \"/api/v1/auth/register\", \"details\": [\"password: size must be between 8 and 100\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный email",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"email: must be a well-formed email address\", \"path\": \"/api/v1/auth/register\", \"details\": [\"email: must be a well-formed email address\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Имя пользователя слишком короткое",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"username: size must be between 3 and 50\", \"path\": \"/api/v1/auth/register\", \"details\": [\"username: size must be between 3 and 50\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Аватар не является изображением",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"avatar: Avatar file must be a non-empty image\", \"path\": \"/api/v1/auth/register\", \"details\": [\"avatar: Avatar file must be a non-empty image\"]}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/register\", \"details\": []}"
                            )
                    )
            )
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Parameter(
                    description = "Данные для регистрации",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = RegisterRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Регистрация без аватара",
                                            value = "{\"username\": \"john_doe\", \"password\": \"securePassword123\", \"name\": \"John Doe\", \"email\": \"john@example.com\", \"location\": \"Moscow\", \"birthDate\": \"1990-05-15\", \"preferredLanguage\": \"RU\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Регистрация с аватаром",
                                            value = "{\"username\": \"john_doe\", \"password\": \"securePassword123\", \"name\": \"John Doe\", \"email\": \"john@example.com\", \"preferredLanguage\": \"EN\", \"avatar\": \"(binary)\"}"
                                    )
                            }
                    )
            )
            @Valid @ModelAttribute RegisterRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя по имени/почте и паролю. Возвращает JWT токен для доступа к защищенным ресурсам."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный вход",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDTO.class),
                            examples = @ExampleObject(
                                    name = "Успешный вход",
                                    value = "{\"token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\", \"type\": \"Bearer\", \"expiresIn\": 86400}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные учетные данные",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неверное имя пользователя или пароль",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"Invalid username or password\", \"path\": \"/api/v1/auth/login\", \"details\": []}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неверные учетные данные",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/auth/login\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/login\", \"details\": []}"
                            )
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Parameter(
                    description = "Учетные данные пользователя",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Пример входа",
                                    value = "{\"login\": \"john_doe\", \"password\": \"securePassword123\"}"
                            )
                    )
            )
            @Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Выход из системы",
            description = "Выполняет выход текущего пользователя из системы. Токен становится недействительным."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Успешный выход",
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/auth/logout\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/logout\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Сброс пароля",
            description = "Сбрасывает пароль пользователя на новый. Требуется указать имя пользователя и новый пароль. " +
                    "ВНИМАНИЕ: Это административная операция - в реальном приложении требуется подтверждение по email."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Пароль успешно сброшен",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пароль слишком короткий",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"newPassword: must not be blank\", \"path\": \"/api/v1/auth/password/reset\", \"details\": [\"newPassword: must not be blank\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Имя пользователя не указано",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"username: must not be blank\", \"path\": \"/api/v1/auth/password/reset\", \"details\": [\"username: must not be blank\"]}"
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пользователь не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found with username: unknown\", \"path\": \"/api/v1/auth/password/reset\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/password/reset\", \"details\": []}"
                            )
                    )
            )
    })
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @Parameter(
                    description = "Данные для сброса пароля",
                    required = true,
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Сброс пароля",
                                    value = "{\"username\": \"john_doe\", \"newPassword\": \"newSecurePassword123\"}"
                            )
                    )
            )
            @Valid @RequestBody PasswordResetRequestDTO request) {
        authService.resetPassword(request.getUsername(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Обновить профиль",
            description = "Обновляет профиль текущего пользователя. Все поля опциональны - укажите только те, которые нужно изменить."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Профиль успешно обновлен",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Некорректный email",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"email: must be a well-formed email address\", \"path\": \"/api/v1/auth/profile\", \"details\": [\"email: must be a well-formed email address\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Дата рождения в будущем",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"birthDate: Birth date must not be in the future\", \"path\": \"/api/v1/auth/profile\", \"details\": [\"birthDate: Birth date must not be in the future\"]}"
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный язык",
                                            value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"preferredLanguage: must not be null\", \"path\": \"/api/v1/auth/profile\", \"details\": [\"preferredLanguage: must not be null\"]}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/auth/profile\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/profile\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @Parameter(
                    description = "Данные для обновления профиля",
                    required = true,
                    content = @Content(
                            examples = {
                                    @ExampleObject(
                                            name = "Обновить имя",
                                            value = "{\"name\": \"John Updated\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Обновить email",
                                            value = "{\"email\": \"newemail@example.com\"}"
                                    ),
                                    @ExampleObject(
                                            name = "Обновить несколько полей",
                                            value = "{\"name\": \"John Doe\", \"email\": \"john@example.com\", \"location\": \"Saint Petersburg\", \"preferredLanguage\": \"EN\"}"
                                    )
                            }
                    )
            )
            @Valid @RequestBody UserProfileUpdateRequestDTO request) {
        userService.updateCurrentUserProfile(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить аккаунт",
            description = "Удаляет аккаунт текущего пользователя безвозвратно. Удаляются все связи с организациями, ролями, инструментами. " +
                    "Файлы, загруженные пользователем, очищаются от идентификатора пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Аккаунт успешно удален",
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/auth/account\", \"details\": []}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 500, \"error\": \"Unexpected error\", \"message\": \"Internal server error\", \"path\": \"/api/v1/auth/account\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {
        userService.deleteCurrentUserAccount();
        authService.logout();
        return ResponseEntity.noContent().build();
    }
}
