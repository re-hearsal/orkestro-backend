package io.github.Romariok.orkestro.organization.repository;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

   List<Organization> findByVisibilityLevel(VisibilityLevelType visibilityLevel);

   List<Organization> findByVisibilityLevelAndNameContainingIgnoreCase(
         VisibilityLevelType visibilityLevel,
         String name);
}
