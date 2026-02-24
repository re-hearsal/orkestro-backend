package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.user.dto.TechnicalRoleCreateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/sections/{sectionId}/roles")
@Tag(name = "Sections - Roles", description = "API для управления техническими ролями секций")
public class SectionTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @Operation(
           summary = "Получить технические роли секции",
           description = "Возвращает список технических ролей, доступных в секции."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "200", description = "Список ролей получен", content = @Content(schema = @Schema(implementation = List.class))),
           @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<List<TechnicalRoleDTO>> getSectionRoles(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId) {
       return ResponseEntity.ok(technicalRoleService.getSectionRoles(sectionId));
    }

    @Operation(
            summary = "Создать техническую роль в секции",
            description = "Создает новую техническую роль для секции."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Роль создана", content = @Content(schema = @Schema(implementation = TechnicalRoleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<TechnicalRoleDTO> createSectionRole(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Valid @RequestBody TechnicalRoleCreateRequestDTO request) {
      TechnicalRoleDTO created = technicalRoleService.createSectionRole(sectionId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }

   @Operation(
           summary = "Удалить техническую роль секции",
           description = "Удаляет техническую роль секции, если она не системная и никому не назначена."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "204", description = "Роль удалена"),
           @ApiResponse(responseCode = "400", description = "Нельзя удалить системную роль или роль, назначенную пользователям", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "404", description = "Секция или роль не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
   @SecurityRequirement(name = "Bearer Authentication")
   @DeleteMapping("/{roleId}")
   public ResponseEntity<Void> deleteSectionRole(
         @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
         @Parameter(description = "ID роли", required = true) @PathVariable @Positive Long roleId) {
      technicalRoleService.deleteSectionRole(sectionId, roleId);
      return ResponseEntity.noContent().build();
   }
}

