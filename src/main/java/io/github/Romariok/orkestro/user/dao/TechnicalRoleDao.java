package io.github.Romariok.orkestro.user.dao;

import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TechnicalRoleDao {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public List<Role> findUserRoles(Long userId) {
        return userRoleRepository.findRolesByUserId(userId);
    }

    public List<Role> findOrganizationRoles(Long organizationId) {
        return roleRepository.findByScopeAndOrganizationId(RoleScopeType.ORGANIZATION, organizationId);
    }

    public List<Role> findSectionRoles(Long sectionId) {
        return roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId);
    }
}


