package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionDTO;
import io.github.Romariok.orkestro.organization.models.OrgFund;
import io.github.Romariok.orkestro.organization.models.OrgFundTransaction;
import io.github.Romariok.orkestro.organization.repository.OrgFundRepository;
import io.github.Romariok.orkestro.organization.repository.OrgFundTransactionRepository;
import io.github.Romariok.orkestro.organization.service.OrgFundService;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class OrgFundServiceTest {

    @Mock
    private OrgFundRepository orgFundRepository;

    @Mock
    private OrgFundTransactionRepository orgFundTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private OrgFundService orgFundService;

    private static final Long ORG_ID = 1L;
    private static final Long USER_ID = 10L;

    private OrgFund fundWithBalance(BigDecimal balance) {
        return OrgFund.builder().organizationId(ORG_ID).balance(balance).build();
    }

    private User performer() {
        return User.builder().id(USER_ID).name("Test User").username("testuser")
                .email("t@t.com").password("pass").build();
    }

    @Test
    void addTransaction_deposit_updatesBalance() {
        OrgFund fund = fundWithBalance(new BigDecimal("100.00"));
        when(orgFundRepository.findById(ORG_ID)).thenReturn(Optional.of(fund));
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        OrgFundTransaction saved = OrgFundTransaction.builder()
                .id(1L).organizationId(ORG_ID).amount(new BigDecimal("50.00"))
                .performedByUserId(USER_ID).createdAt(Instant.now()).build();
        when(orgFundTransactionRepository.save(any())).thenReturn(saved);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(performer()));

        OrgFundTransactionDTO result = orgFundService.addTransaction(ORG_ID, new BigDecimal("50.00"), "deposit");

        ArgumentCaptor<OrgFund> captor = ArgumentCaptor.forClass(OrgFund.class);
        verify(orgFundRepository).save(captor.capture());
        assertEquals(new BigDecimal("150.00"), captor.getValue().getBalance());
        assertEquals(new BigDecimal("50.00"), result.getAmount());
        assertEquals("Test User", result.getPerformedByName());
    }

    @Test
    void addTransaction_withdrawal_updatesBalance() {
        OrgFund fund = fundWithBalance(new BigDecimal("200.00"));
        when(orgFundRepository.findById(ORG_ID)).thenReturn(Optional.of(fund));
        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);
        OrgFundTransaction saved = OrgFundTransaction.builder()
                .id(2L).organizationId(ORG_ID).amount(new BigDecimal("-100.00"))
                .performedByUserId(USER_ID).createdAt(Instant.now()).build();
        when(orgFundTransactionRepository.save(any())).thenReturn(saved);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(performer()));

        orgFundService.addTransaction(ORG_ID, new BigDecimal("-100.00"), "withdrawal");

        ArgumentCaptor<OrgFund> captor = ArgumentCaptor.forClass(OrgFund.class);
        verify(orgFundRepository).save(captor.capture());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getBalance());
    }

    @Test
    void addTransaction_negativeBalance_throwsBusinessException() {
        OrgFund fund = fundWithBalance(new BigDecimal("50.00"));
        when(orgFundRepository.findById(ORG_ID)).thenReturn(Optional.of(fund));

        assertThrows(BusinessException.class, () ->
                orgFundService.addTransaction(ORG_ID, new BigDecimal("-100.00"), "overdraft"));
    }

    @Test
    void addTransaction_fundNotFound_throwsEntityNotFoundException() {
        when(orgFundRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                orgFundService.addTransaction(ORG_ID, new BigDecimal("10.00"), "desc"));
    }

    @Test
    void exportTransactionsCsv_returnsValidCsv() {
        Instant from = Instant.EPOCH;
        Instant to = Instant.now();
        OrgFundTransaction t = OrgFundTransaction.builder()
                .id(1L).organizationId(ORG_ID).amount(new BigDecimal("75.00"))
                .description("test").performedByUserId(USER_ID).createdAt(Instant.now()).build();
        when(orgFundTransactionRepository.findByOrganizationIdAndCreatedAtBetween(ORG_ID, from, to))
                .thenReturn(List.of(t));
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(performer()));

        byte[] csv = orgFundService.exportTransactionsCsv(ORG_ID, from, to);

        assertNotNull(csv);
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(true, content.startsWith("id,amount,description,performed_by,created_at\n"));
        assertEquals(true, content.contains("75.00"));
        assertEquals(true, content.contains("Test User"));
    }
}
