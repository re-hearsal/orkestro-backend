package io.github.Romariok.orkestro.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.organization.models.OrganizationLink;

@Repository
public interface OrganizationLinkRepository extends JpaRepository<OrganizationLink, Long> {

   List<OrganizationLink> findByOrganizationId(Long organizationId);

   void deleteByOrganizationId(Long organizationId);
}
