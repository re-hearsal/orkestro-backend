package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
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
@RequestMapping("/api/v1/organizations/{organizationId}/sections")
@Tag(name = "Organizations - Sections", description = "API для управления секциями организации")
public class OrganizationSectionController {

        private final SectionService sectionService;

        @Operation(summary = "Получить секции организации", description = "Возвращает список секций организации. Доступно только участникам организации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Список секций получен", content = @Content(schema = @Schema(implementation = SectionDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @GetMapping
        public ResponseEntity<List<SectionDTO>> getSectionsByOrganization(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId) {
                return ResponseEntity.ok(sectionService.getSectionsByOrganization(organizationId));
        }

        @Operation(summary = "Создать секцию в организации", description = "Создает новую секцию верхнего уровня в организации.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Секция создана", content = @Content(schema = @Schema(implementation = SectionDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Организация не найдена", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
                        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        })
        @SecurityRequirement(name = "Bearer Authentication")
        @PostMapping
        public ResponseEntity<SectionDTO> createSectionInOrganization(
                        @Parameter(description = "ID организации", required = true) @PathVariable @Positive Long organizationId,
                        @Valid @RequestBody SectionCreateRequestDTO request) {
                SectionDTO created = sectionService.createSectionInOrganization(organizationId, request);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
}
