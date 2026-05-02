package io.github.Romariok.orkestro.user.controller;

import io.github.Romariok.orkestro.user.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.user.dto.PublicUserProfileDTO;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "API для управления пользователями и их профилями")
public class PublicUserController {

    private final UserService userService;
    private final MusicalRoleService musicalRoleService;

    @Operation(
            summary = "Получить публичный профиль пользователя",
            description = "Возвращает публичную информацию о профиле пользователя по его ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Публичный профиль пользователя успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PublicUserProfileDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = "{\"id\": 1, \"username\": \"john_doe\", \"name\": \"John Doe\", \"email\": \"john@example.com\", \"location\": \"Moscow\", \"birthDate\": \"1990-05-15\", \"preferredLanguage\": \"RU\", \"profileImageFileId\": 123}"
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
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 401, \"error\": \"Authentication failed\", \"message\": \"Bad credentials\", \"path\": \"/api/v1/users/1\", \"details\": []}"
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
                                    name = "Пользователь не найден",
                                    value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 404, \"error\": \"Entity not found\", \"message\": \"User not found: 999\", \"path\": \"/api/v1/users/999\", \"details\": []}"
                            )
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{userId}")
    public ResponseEntity<PublicUserProfileDTO> getPublicUserProfile(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(userService.getPublicUserProfile(userId));
    }

    @Operation(
            summary = "Получить музыкальные роли пользователя",
            description = "Возвращает список инструментов пользователя по его ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список инструментов пользователя успешно получен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MusicalRoleDTO.class)
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
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{userId}/musical-roles")
    public ResponseEntity<List<MusicalRoleDTO>> getUserMusicalRoles(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(musicalRoleService.getUserMusicalRoles(userId));
    }
}
