package io.github.Romariok.orkestro.organization.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OrgFundRealtimeSnapshotDTO {
    private Long organizationId;
    private BigDecimal balance;
    private List<OrgFundTransactionDTO> transactions;
    private long totalTransactions;
}
