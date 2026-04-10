package io.github.Romariok.orkestro.organization.controller;

import io.github.Romariok.orkestro.organization.dto.OrgFundDTO;
import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionDTO;
import io.github.Romariok.orkestro.organization.service.OrgFundService;
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
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/fund")
@Tag(name = "Organization Fund", description = "API для управления денежным фондом организации")
@SecurityRequirement(name = "bearerAuth")
public class OrgFundController {

    private final OrgFundService orgFundService;

    @Operation(summary = "Получить баланс фонда", description = "Возвращает текущий баланс фонда организации. Требует членства в организации.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Баланс успешно получен",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrgFundDTO.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Организация не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<OrgFundDTO> getBalance(
            @Parameter(description = "ID организации") @PathVariable Long organizationId) {
        return ResponseEntity.ok(orgFundService.getBalance(organizationId));
    }

    @Operation(summary = "Добавить транзакцию", description = "Пополнение (amount > 0) или снятие (amount < 0) средств из фонда. Требует права ORG_FUND_MANIPULATION.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Транзакция успешно создана",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrgFundTransactionDTO.class))),
            @ApiResponse(responseCode = "400", description = "Недостаточно средств или невалидные данные",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/transactions")
    public ResponseEntity<OrgFundTransactionDTO> addTransaction(
            @Parameter(description = "ID организации") @PathVariable Long organizationId,
            @Valid @RequestBody OrgFundTransactionCreateRequestDTO request) {
        OrgFundTransactionDTO result = orgFundService.addTransaction(
                organizationId, request.getAmount(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Получить транзакции", description = "Возвращает постраничный список транзакций фонда за указанный период. Требует членства в организации.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список транзакций получен"),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/transactions")
    public ResponseEntity<Page<OrgFundTransactionDTO>> getTransactions(
            @Parameter(description = "ID организации") @PathVariable Long organizationId,
            @Parameter(description = "Начало периода (ISO-8601)") @RequestParam(required = false) Instant dateFrom,
            @Parameter(description = "Конец периода (ISO-8601)") @RequestParam(required = false) Instant dateTo,
            @PageableDefault(size = 20) Pageable pageable) {
        Instant from = dateFrom != null ? dateFrom : Instant.EPOCH;
        Instant to = dateTo != null ? dateTo : Instant.now().plusSeconds(86400);
        return ResponseEntity.ok(orgFundService.getTransactions(organizationId, from, to, pageable));
    }

    @Operation(summary = "Экспорт транзакций в CSV", description = "Возвращает файл CSV со всеми транзакциями фонда за указанный период. Требует членства в организации.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV-файл успешно сформирован"),
            @ApiResponse(responseCode = "403", description = "Нет доступа",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/transactions/export")
    public ResponseEntity<byte[]> exportTransactionsCsv(
            @Parameter(description = "ID организации") @PathVariable Long organizationId,
            @Parameter(description = "Начало периода (ISO-8601)") @RequestParam(required = false) Instant dateFrom,
            @Parameter(description = "Конец периода (ISO-8601)") @RequestParam(required = false) Instant dateTo) {
        Instant from = dateFrom != null ? dateFrom : Instant.EPOCH;
        Instant to = dateTo != null ? dateTo : Instant.now().plusSeconds(86400);
        byte[] csv = orgFundService.exportTransactionsCsv(organizationId, from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("transactions.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv);
    }
}
