package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.role.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByScopeAndOrganizationId(RoleScopeType scope, Long organizationId);

    List<Role> findByScopeAndSectionId(RoleScopeType scope, Long sectionId);

    List<Role> findByScopeAndSystemTrue(RoleScopeType scope);

    Optional<Role> findByScopeAndOrganizationIdAndName(RoleScopeType scope, Long organizationId, String name);

    Optional<Role> findByScopeAndSectionIdAndName(RoleScopeType scope, Long sectionId, String name);
}
