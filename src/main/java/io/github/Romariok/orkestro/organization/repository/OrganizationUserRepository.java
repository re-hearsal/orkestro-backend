package io.github.Romariok.orkestro.organization.repository;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.OrganizationUserId;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, OrganizationUserId>,
      JpaSpecificationExecutor<OrganizationUser> {

   Optional<OrganizationUser> findByOrganizationIdAndUserId(Long organizationId, Long userId);

   List<OrganizationUser> findByOrganizationIdAndStatus(Long organizationId, OrganizationUserStatusType status);

   List<OrganizationUser> findByOrganizationIdAndStatusOrderByJoinedAtAsc(
         Long organizationId, OrganizationUserStatusType status);

   long countByOrganizationIdAndStatus(Long organizationId, OrganizationUserStatusType status);

   @Override
   @EntityGraph(attributePaths = "user")
   Page<OrganizationUser> findAll(Specification<OrganizationUser> spec, Pageable pageable);

   void deleteByUserId(Long userId);

   void deleteByOrganizationId(Long organizationId);

   void deleteByOrganizationIdAndUserId(Long organizationId, Long userId);
}
