package io.github.Romariok.orkestro.user.repository;

import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByScopeAndOrganizationId(RoleScopeType scope, Long organizationId);

    List<Role> findByScopeAndSectionId(RoleScopeType scope, Long sectionId);

    List<Role> findByScopeAndSectionIdIn(RoleScopeType scope, Collection<Long> sectionIds);

    List<Role> findByScopeAndSystemTrue(RoleScopeType scope);

    Optional<Role> findByScopeAndOrganizationIdAndName(RoleScopeType scope, Long organizationId, String name);

    Optional<Role> findByScopeAndSectionIdAndName(RoleScopeType scope, Long sectionId, String name);
}
