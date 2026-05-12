package io.github.Romariok.orkestro.organization.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrgFundTransactionCreateRequestDTO {

    @NotNull
    private BigDecimal amount;

    private String description;
}
