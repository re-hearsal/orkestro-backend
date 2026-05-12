package io.github.Romariok.orkestro.organization.repository;

import io.github.Romariok.orkestro.organization.models.Organization;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
   boolean existsByProfileImageFileId(Long profileImageFileId);

   List<Organization> findByNameContainingIgnoreCase(String name);

   Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
