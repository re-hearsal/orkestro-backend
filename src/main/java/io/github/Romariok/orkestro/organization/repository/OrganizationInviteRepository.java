package io.github.Romariok.orkestro.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.organization.models.OrganizationInvite;
import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {

    Optional<OrganizationInvite> findByCode(String code);
}
