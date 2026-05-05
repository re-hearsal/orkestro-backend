package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.dto.OrgFundDTO;
import io.github.Romariok.orkestro.organization.dto.OrgFundTransactionDTO;
import io.github.Romariok.orkestro.organization.models.OrgFund;
import io.github.Romariok.orkestro.organization.models.OrgFundTransaction;
import io.github.Romariok.orkestro.organization.repository.OrgFundRepository;
import io.github.Romariok.orkestro.organization.repository.OrgFundTransactionRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrgFundService {

    private final OrgFundRepository orgFundRepository;
    private final OrgFundTransactionRepository orgFundTransactionRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
    public OrgFundDTO getBalance(Long organizationId) {
        OrgFund fund = orgFundRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Fund not found for organization: " + organizationId));
        return OrgFundDTO.builder()
                .organizationId(fund.getOrganizationId())
                .balance(fund.getBalance())
                .build();
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_FUND_MANIPULATION')")
    public OrgFundTransactionDTO addTransaction(Long organizationId, BigDecimal amount, String description) {
        OrgFund fund = orgFundRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Fund not found for organization: " + organizationId));

        BigDecimal newBalance = fund.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Transaction would result in negative balance");
        }

        fund.setBalance(newBalance);
        orgFundRepository.save(fund);

        Long performedByUserId = securityUtils.getCurrentUserId();
        OrgFundTransaction transaction = OrgFundTransaction.builder()
                .organizationId(organizationId)
                .amount(amount)
                .description(description)
                .performedByUserId(performedByUserId)
                .build();
        OrgFundTransaction saved = orgFundTransactionRepository.save(transaction);

        User performer = userRepository.findById(performedByUserId).orElse(null);
        return toDTO(saved, performer);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
    public Page<OrgFundTransactionDTO> getTransactions(Long organizationId, Instant dateFrom, Instant dateTo, Pageable pageable) {
        Page<OrgFundTransaction> page = orgFundTransactionRepository
                .findByOrganizationIdAndCreatedAtBetween(organizationId, dateFrom, dateTo, pageable);

        List<Long> userIds = page.getContent().stream()
                .map(OrgFundTransaction::getPerformedByUserId)
                .distinct()
                .toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return page.map(t -> toDTO(t, usersById.get(t.getPerformedByUserId())));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
    public byte[] exportTransactionsCsv(Long organizationId, Instant dateFrom, Instant dateTo) {
        List<OrgFundTransaction> transactions = orgFundTransactionRepository
                .findByOrganizationIdAndCreatedAtBetween(organizationId, dateFrom, dateTo);

        List<Long> userIds = transactions.stream()
                .map(OrgFundTransaction::getPerformedByUserId)
                .distinct()
                .toList();
        Map<Long, String> nameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            writer.write("id,amount,description,performed_by,created_at\n");
            for (OrgFundTransaction t : transactions) {
                writer.write(t.getId() + ","
                        + t.getAmount() + ","
                        + escapeCsv(t.getDescription()) + ","
                        + escapeCsv(nameById.getOrDefault(t.getPerformedByUserId(), "")) + ","
                        + t.getCreatedAt() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
        return baos.toByteArray();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private OrgFundTransactionDTO toDTO(OrgFundTransaction t, User performer) {
        return OrgFundTransactionDTO.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .description(t.getDescription())
                .performedByUserId(t.getPerformedByUserId())
                .performedByName(performer != null ? performer.getName() : null)
                .performedByProfileImageFileId(performer != null ? performer.getProfileImageFileId() : null)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
