package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.Role;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByScopeAndOrganizationId(RoleScopeType scope, Long organizationId);

    List<Role> findByScopeAndSectionId(RoleScopeType scope, Long sectionId);

    List<Role> findByScopeAndSystemTrue(RoleScopeType scope);
}


