package io.github.Romariok.orkestro.organization.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.dto.OrgFundDTO;
import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionDTO;
import io.github.Romariok.orkestro.organization.service.OrgFundService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrgFundControllerTest {

    @Mock
    private OrgFundService orgFundService;

    @InjectMocks
    private OrgFundController orgFundController;

    @Test
    void getBalance_returnsOk() {
        OrgFundDTO response = OrgFundDTO.builder()
                .organizationId(1L)
                .balance(new BigDecimal("1500.00"))
                .build();
        when(orgFundService.getBalance(eq(1L))).thenReturn(response);

        var result = orgFundController.getBalance(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getOrganizationId());
        assertEquals(new BigDecimal("1500.00"), result.getBody().getBalance());
    }

    @Test
    void addTransaction_returnsCreated() {
        OrgFundTransactionDTO response = OrgFundTransactionDTO.builder()
                .id(10L)
                .amount(new BigDecimal("500.00"))
                .description("Membership fee")
                .performedByUserId(42L)
                .performedByName("Ivan Petrov")
                .createdAt(Instant.parse("2026-03-01T10:00:00Z"))
                .build();
        when(orgFundService.addTransaction(eq(1L), any(BigDecimal.class), any())).thenReturn(response);

        OrgFundTransactionCreateRequestDTO request = new OrgFundTransactionCreateRequestDTO(
                new BigDecimal("500.00"), "Membership fee");

        var result = orgFundController.addTransaction(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertEquals(new BigDecimal("500.00"), result.getBody().getAmount());
    }

    @Test
    void getTransactions_returnsOk() {
        List<OrgFundTransactionDTO> transactions = List.of(
                OrgFundTransactionDTO.builder().id(1L).amount(new BigDecimal("200.00")).build(),
                OrgFundTransactionDTO.builder().id(2L).amount(new BigDecimal("-50.00")).build()
        );
        Page<OrgFundTransactionDTO> page = new PageImpl<>(transactions);
        when(orgFundService.getTransactions(eq(1L), any(Instant.class), any(Instant.class), any()))
                .thenReturn(page);

        var result = orgFundController.getTransactions(1L, null, null, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTotalElements());
    }
}
