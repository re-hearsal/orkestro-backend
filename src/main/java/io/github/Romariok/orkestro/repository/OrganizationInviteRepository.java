package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.organization.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {
}
