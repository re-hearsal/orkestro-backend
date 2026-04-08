package io.github.Romariok.orkestro.organization.controller;

import io.github.Romariok.orkestro.organization.dto.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationJoinRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationJoinCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.service.OrganizationService;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "API для управления организациями (музыкальными группами)")
public class OrganizationController {

        private final OrganizationService organizationService;
        private final OrganizationUserService organizationUserService;

        @Operation(summary = "Создать организацию", description = "Создает новую организацию (музыкальную группу). Создатель автоматически становится администратором организации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Организация успешно создана", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationDTO.class), examples = @ExampleObject(name = "Пример ответа", value = "{\"id\": 1, \"name\": \"Rock Band\", \"location\": \"Moscow\", \"description\": \"Professional rock band\", \"profileImageFileId\": 123, \"createdAt\": \"2026-02-18T10:00:00\"}"))),
                        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class), examples = {
                                        @ExampleObject(name = "Название слишком короткое", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"name: size must be between 3 and 255\", \"path\": \"/api/v1/organizations\", \"details\": [\"name: size must be between 3 and 255\"]}"),
                                        @ExampleObject(name = "Профиль не является изображением", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"Profile image must be a non-empty image\", \"path\": \"/api/v1/organizations\", \"details\": []}"),
                                        @ExampleObject(name = "Дубликаты ссылок", value = "{\"timestamp\": \"2026-02-18T10:00:00Z\", \"status\": 400, \"error\": \"Validation failed\", \"message\": \"Links must not contain duplicates\", \"path\": \"/api/v1/organizations\", \"details\": []}")
                        })),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<OrganizationDTO> createOrganization(
                        @Parameter(description = "Данные для создания организации", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = OrganizationCreateRequestDTO.class), examples = {
                                        @ExampleObject(name = "Организация", value = "{\"name\": \"Rock Band\", \"location\": \"Moscow\", \"description\": \"Professional rock band\"}"),
                                        @ExampleObject(name = "Минимальные поля", value = "{\"name\": \"Ensemble\", \"location\": \"Saint Petersburg\"}")
                        })) @Valid @ModelAttribute OrganizationCreateRequestDTO request) {
                OrganizationDTO created = organizationService.createOrganization(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        @Operation(summary = "Получить организацию", description = "Возвращает информацию об организации по её ID.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Организация найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping("/{organizationId}")
        public ResponseEntity<OrganizationDTO> getOrganization(
                        @Parameter(description = "ID организации", required = true, example = "1") @PathVariable @Positive Long organizationId) {
                return ResponseEntity.ok(organizationService.getOrganization(organizationId));
        }

        @Operation(summary = "Обновить организацию", description = "Обновляет информацию об организации. Все поля опциональны.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Организация обновлена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = OrganizationDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PatchMapping("/{organizationId}")
        public ResponseEntity<OrganizationDTO> updateOrganization(
                        @Parameter(description = "ID организации", required = true, example = "1") @PathVariable @Positive Long organizationId,
                        @Parameter(description = "Данные для обновления", required = true, content = @Content(examples = {
                                        @ExampleObject(name = "Обновить описание", value = "{\"description\": \"New description\"}"),
                                        @ExampleObject(name = "Обновить несколько полей", value = "{\"name\": \"New Name\", \"location\": \"New York\"}")
                        })) @Valid @RequestBody OrganizationUpdateRequestDTO request) {
                return ResponseEntity.ok(organizationService.updateOrganization(organizationId, request));
        }

        @Operation(summary = "Удалить изображение профиля организации", description = "Удаляет изображение профиля организации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Изображение удалено", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @DeleteMapping("/{organizationId}/profile-image")
        public ResponseEntity<Void> deleteOrganizationProfileImage(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
                organizationService.deleteOrganizationProfileImage(organizationId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Удалить организацию", description = "Удаляет организацию. Доступно только для администраторов.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Организация удалена", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @DeleteMapping("/{organizationId}")
        public ResponseEntity<Void> deleteOrganization(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
                organizationService.deleteOrganization(organizationId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Покинуть организацию", description = "Текущий пользователь покидает организацию.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Пользователь покинул организацию", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Последний лидер не может покинуть организацию", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @DeleteMapping("/{organizationId}/members/me")
        public ResponseEntity<Void> leaveOrganization(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
                organizationUserService.leaveCurrentOrganization(organizationId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Присоединиться к организации", description = "Отправляет запрос на присоединение к организации с описанием (до 1000 символов).")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Запрос отправлен", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Организация приватная", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Уже состоите в организации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping("/{organizationId}/join")
        public ResponseEntity<Void> joinPublicOrganization(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Parameter(description = "Данные заявки на вступление", required = true, content = @Content(examples = @ExampleObject(value = "{\"description\": \"Играю на трубе 4 года, хочу участвовать в репетициях по выходным.\"}"))) @Valid @RequestBody OrganizationJoinCreateRequestDTO request) {
                organizationUserService.requestToJoinOrganization(organizationId, request.getDescription());
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Удалить участника", description = "Удаляет пользователя из организации. Доступно для администраторов.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Участник удален", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Нельзя удалить последнего админа", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация или пользователь не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @DeleteMapping("/{organizationId}/members/{userId}")
        public ResponseEntity<Void> removeMember(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId) {
                organizationUserService.removeUserFromOrganization(organizationId, userId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Поиск участников (с пагинацией)", description = "Возвращает страницу участников организации с возможностью фильтрации по имени, ролям и инструментам.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список участников получен", content = @Content(schema = @Schema(implementation = Page.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping("/{organizationId}/members/page")
        public ResponseEntity<Page<OrganizationMemberDTO>> searchMembersPage(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Parameter(description = "Поиск по имени") @RequestParam(required = false) String query,
                        @Parameter(description = "Фильтр по ID ролей") @RequestParam(required = false) List<@Positive Long> roleIds,
                        @Parameter(description = "Фильтр по ID инструментов") @RequestParam(required = false) List<@Positive Long> instrumentIds,
                        @Parameter(description = "Параметры пагинации") @PageableDefault(size = 20) Pageable pageable) {
                return ResponseEntity.ok(
                                organizationUserService.searchMembers(organizationId, query, roleIds, instrumentIds,
                                                pageable));
        }

        @Operation(summary = "Получить ожидающие запросы на вступление", description = "Возвращает список пользователей, ожидающих одобрения вступления в организацию.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список запросов получен", content = @Content(schema = @Schema(implementation = List.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping("/{organizationId}/join-requests/pending")
        public ResponseEntity<List<OrganizationJoinRequestDTO>> getPendingJoinRequests(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
                List<OrganizationUser> pending = organizationUserService.getPendingJoinRequests(organizationId);
                List<OrganizationJoinRequestDTO> dtos = pending.stream()
                                .map(OrganizationJoinRequestDTO::fromEntity)
                                .toList();
                return ResponseEntity.ok(dtos);
        }

        @Operation(summary = "Одобрить запрос на вступление", description = "Одобряет запрос пользователя на вступление в организацию.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Запрос одобрен", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Запрос уже обработан", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация или запрос не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping("/{organizationId}/join-requests/{userId}/approve")
        public ResponseEntity<Void> approveJoinRequest(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId) {
                organizationUserService.approveJoinRequest(organizationId, userId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Отклонить запрос на вступление", description = "Отклоняет запрос пользователя на вступление в организацию.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Запрос отклонен", content = @Content),
                        @ApiResponse(responseCode = "400", description = "Запрос уже обработан", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация или запрос не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping("/{organizationId}/join-requests/{userId}/reject")
        public ResponseEntity<Void> rejectJoinRequest(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId) {
                organizationUserService.rejectJoinRequest(organizationId, userId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Поиск организаций", description = "Возвращает список организаций по названию. Не требует авторизации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список организаций получен", content = @Content(schema = @Schema(implementation = List.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @GetMapping("/public")
        public ResponseEntity<List<OrganizationDTO>> searchPublicOrganizations(
                        @Parameter(description = "Поиск по названию") @RequestParam(required = false) String name) {
                return ResponseEntity.ok(organizationService.searchPublicOrganizationsByName(name));
        }

        @Operation(summary = "Поиск организаций (с пагинацией)", description = "Возвращает страницу организаций. Не требует авторизации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список организаций получен", content = @Content(schema = @Schema(implementation = Page.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @GetMapping("/public/page")
        public ResponseEntity<Page<OrganizationDTO>> searchPublicOrganizationsPage(
                        @Parameter(description = "Поиск по названию") @RequestParam(required = false) String name,
                        @Parameter(description = "Параметры пагинации") @PageableDefault(size = 10) Pageable pageable) {
                return ResponseEntity.ok(organizationService.searchPublicOrganizationsByName(name, pageable));
        }
}
