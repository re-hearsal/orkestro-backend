package io.github.Romariok.orkestro.organization.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.organization.models.OrganizationLink;

@Repository
public interface OrganizationLinkRepository extends JpaRepository<OrganizationLink, Long> {

   List<OrganizationLink> findByOrganizationId(Long organizationId);

   @Modifying(flushAutomatically = true, clearAutomatically = true)
   @Query("DELETE FROM OrganizationLink l WHERE l.organizationId = :organizationId")
   void deleteByOrganizationId(@Param("organizationId") Long organizationId);
}
