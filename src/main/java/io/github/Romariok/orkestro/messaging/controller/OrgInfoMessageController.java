package io.github.Romariok.orkestro.messaging.controller;

import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageCreateRequestDTO;
import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageDTO;
import io.github.Romariok.orkestro.messaging.service.OrgInfoMessageService;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/info-messages")
@Tag(name = "Organization Info Messages", description = "API для информационных сообщений организации")
@SecurityRequirement(name = "bearerAuth")
public class OrgInfoMessageController {

    private final OrgInfoMessageService orgInfoMessageService;

    @Operation(summary = "Опубликовать информационное сообщение в организацию",
            description = "Публикует сообщение от имени текущего пользователя. Требует права ORG_WRITE_INFO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Сообщение успешно создано",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrgInfoMessageDTO.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<OrgInfoMessageDTO> postMessage(
            @Parameter(description = "ID организации") @PathVariable Long organizationId,
            @Valid @RequestBody OrgInfoMessageCreateRequestDTO request) {
        OrgInfoMessageDTO result = orgInfoMessageService.postOrgMessage(organizationId, request.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Получить информационные сообщения организации",
            description = "Возвращает постраничный список сообщений организации. Требует членства в организации.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список сообщений получен"),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<OrgInfoMessageDTO>> getMessages(
            @Parameter(description = "ID организации") @PathVariable Long organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(orgInfoMessageService.getOrgMessages(organizationId, pageable));
    }
}
