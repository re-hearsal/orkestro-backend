package io.github.Romariok.orkestro.organization.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
public class OrgFundTransactionDTO {
    private Long id;
    private BigDecimal amount;
    private String description;
    private Long performedByUserId;
    private String performedByName;
    private Long performedByProfileImageFileId;
    private Instant createdAt;
}
