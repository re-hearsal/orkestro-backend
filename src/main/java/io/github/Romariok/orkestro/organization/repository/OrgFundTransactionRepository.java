package io.github.Romariok.orkestro.organization.repository;

import io.github.Romariok.orkestro.organization.models.OrgFundTransaction;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgFundTransactionRepository extends JpaRepository<OrgFundTransaction, Long> {

    Page<OrgFundTransaction> findByOrganizationIdAndCreatedAtBetween(
            Long organizationId, Instant from, Instant to, Pageable pageable);

    java.util.List<OrgFundTransaction> findByOrganizationIdAndCreatedAtBetween(
            Long organizationId, Instant from, Instant to);
}
