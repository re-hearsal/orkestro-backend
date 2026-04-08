package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.section.dto.SectionMemberDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@RequestMapping("/api/v1/sections/{sectionId}/members")
@Tag(name = "Sections - Members", description = "API для управления участниками секций")
public class SectionMemberController {

   private final SectionService sectionService;

   @Operation(
           summary = "Добавить участника в секцию",
           description = "Добавляет пользователя в секцию."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "204", description = "Участник добавлен", content = @Content),
           @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "404", description = "Секция или пользователь не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/{userId}")
    public ResponseEntity<Void> addUserToSection(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId) {
      sectionService.addUserToSection(sectionId, userId);
      return ResponseEntity.noContent().build();
   }

    @Operation(
            summary = "Удалить участника из секции",
            description = "Удаляет пользователя из секции."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Участник удален", content = @Content),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Секция или пользователь не найдены", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeUserFromSection(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Parameter(description = "ID пользователя", required = true) @PathVariable @Positive Long userId) {
      sectionService.removeUserFromSection(sectionId, userId);
      return ResponseEntity.noContent().build();
   }

    @Operation(
            summary = "Покинуть секцию",
            description = "Текущий аутентифицированный пользователь покидает секцию. Лидер не может покинуть секцию, пока есть другие участники."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Секция покинута", content = @Content),
            @ApiResponse(responseCode = "400", description = "Нарушение бизнес-правил (лидер не может покинуть секцию)", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveSection(
            @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId) {
        sectionService.leaveCurrentSection(sectionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Поиск участников секции",
            description = "Возвращает страницу участников секции с возможностью фильтрации по имени, ролям и инструментам."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список участников получен", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Секция не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/page")
    public ResponseEntity<Page<SectionMemberDTO>> searchMembersPage(
          @Parameter(description = "ID секции", required = true) @PathVariable @Positive Long sectionId,
          @Parameter(description = "Поиск по имени") @RequestParam(required = false) String query,
          @Parameter(description = "Фильтр по ID ролей") @RequestParam(required = false) List<@Positive Long> roleIds,
          @Parameter(description = "Фильтр по ID инструментов") @RequestParam(required = false) List<@Positive Long> instrumentIds,
          @Parameter(description = "Параметры пагинации") @PageableDefault(size = 20) Pageable pageable) {
      return ResponseEntity.ok(
            sectionService.searchMembers(
                  sectionId,
                  query,
                  roleIds,
                  instrumentIds,
                  pageable));
   }
}

