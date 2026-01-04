package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.organization.OrganizationLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationLinkRepository extends JpaRepository<OrganizationLink, Long> {

   List<OrganizationLink> findByOrganizationId(Long organizationId);

   void deleteByOrganizationId(Long organizationId);
}
