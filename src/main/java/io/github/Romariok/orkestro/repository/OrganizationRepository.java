package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.models.organization.Organization;
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
