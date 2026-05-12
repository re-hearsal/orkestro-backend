package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/api/v1/sections/{sectionId}/members/{userId}/roles")
@Tag(name = "Sections - Member Roles", description = "API для управления техническими ролями участников секции")
public class SectionMemberTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @Operation(
           summary = "Назначить техническую роль участнику секции",
           description = "Назначает техническую роль пользователю в рамках секции."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "204", description = "Роль назначена", content = @Content),
           @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "404", description = "Секция, пользователь или роль не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{roleId}")
    public ResponseEntity<Void> assignSectionRoleToUser(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId,
          @Parameter(description = "ID роли", required = true) @PathVariable @Positive Long roleId) {
       technicalRoleService.assignSectionRoleToUser(sectionId, userId, roleId);
       return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Удалить техническую роль у участника секции",
            description = "Удаляет техническую роль у пользователя в рамках секции."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Роль удалена", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Секция, пользователь или роль не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> removeSectionRoleFromUser(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId,
          @Parameter(description = "ID роли", required = true) @PathVariable @Positive Long roleId) {
      technicalRoleService.removeSectionRoleFromUser(sectionId, userId, roleId);
      return ResponseEntity.noContent().build();
   }
}

