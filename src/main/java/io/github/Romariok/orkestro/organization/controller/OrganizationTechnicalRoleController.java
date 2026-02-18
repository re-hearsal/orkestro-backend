package io.github.Romariok.orkestro.organization.controller;

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
@RequestMapping("/api/v1/organizations/{organizationId}/roles")
@Tag(name = "Organizations - Roles", description = "API для управления техническими ролями организации")
public class OrganizationTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @Operation(
           summary = "Получить технические роли организации",
           description = "Возвращает список технических ролей, доступных в организации."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "200", description = "Список ролей получен", content = @Content(schema = @Schema(implementation = List.class))),
           @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    public ResponseEntity<List<TechnicalRoleDTO>> getOrganizationRoles(
          @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
       return ResponseEntity.ok(technicalRoleService.getOrganizationRoles(organizationId));
    }

    @Operation(
            summary = "Создать техническую роль в организации",
            description = "Создает новую техническую роль для организации."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Роль создана", content = @Content(schema = @Schema(implementation = TechnicalRoleDTO.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping
    public ResponseEntity<TechnicalRoleDTO> createOrganizationRole(
          @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
          @Valid @RequestBody TechnicalRoleCreateRequestDTO request) {
      TechnicalRoleDTO created = technicalRoleService.createOrganizationRole(organizationId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }
}

