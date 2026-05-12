package io.github.Romariok.orkestro.messaging.repository;

import io.github.Romariok.orkestro.messaging.models.OrgInfoMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgInfoMessageRepository extends JpaRepository<OrgInfoMessage, Long> {

    Page<OrgInfoMessage> findByOrganizationIdAndSectionIdIsNullOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    Page<OrgInfoMessage> findBySectionIdOrderByCreatedAtDesc(Long sectionId, Pageable pageable);

    void deleteByOrganizationId(Long organizationId);
}
