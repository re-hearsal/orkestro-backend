package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.organization.OrganizationUserId;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, OrganizationUserId> {

   Optional<OrganizationUser> findByOrganizationIdAndUserId(Long organizationId, Long userId);

   List<OrganizationUser> findByOrganizationIdAndStatus(Long organizationId, OrganizationUserStatusType status);

   long countByOrganizationIdAndStatus(Long organizationId, OrganizationUserStatusType status);

   void deleteByUserId(Long userId);

   void deleteByOrganizationId(Long organizationId);

   void deleteByOrganizationIdAndUserId(Long organizationId, Long userId);
}
