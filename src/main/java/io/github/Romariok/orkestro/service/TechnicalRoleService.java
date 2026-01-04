package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.dto.role.TechnicalRoleDTO;
import io.github.Romariok.orkestro.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.user.UserRole;
import io.github.Romariok.orkestro.models.user.UserRoleId;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SectionUserRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TechnicalRoleService {

    private final TechnicalRoleDao technicalRoleDao;
    private final TechnicalRoleMapper technicalRoleMapper;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final SectionUserRepository sectionUserRepository;

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getUserRoles(Long userId) {
        List<Role> roles = technicalRoleDao.findUserRoles(userId);
        return technicalRoleMapper.toDtoList(roles);
    }

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getOrganizationRoles(Long organizationId) {
        List<Role> roles = technicalRoleDao.findOrganizationRoles(organizationId);
        return technicalRoleMapper.toDtoList(roles);
    }

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getSectionRoles(Long sectionId) {
        List<Role> roles = technicalRoleDao.findSectionRoles(sectionId);
        return technicalRoleMapper.toDtoList(roles);
    }

    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_ASSIGN_TECH_ROLE')")
    public void assignOrganizationRoleToUser(Long organizationId, Long userId, Long roleId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.ORGANIZATION || role.getOrganizationId() == null
                || !role.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Role " + roleId + " does not belong to organization " + organizationId);
        }

        organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + organizationId));

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();

        if (userRoleRepository.existsById(id)) {
            return;
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .build();
        userRoleRepository.save(userRole);
    }

    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_ASSIGN_TECH_ROLE')")
    public void removeOrganizationRoleFromUser(Long organizationId, Long userId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.ORGANIZATION || role.getOrganizationId() == null
                || !role.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Role " + roleId + " does not belong to organization " + organizationId);
        }

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        userRoleRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_ASSIGN_TECH_ROLE')")
    public void assignSectionRoleToUser(Long sectionId, Long userId, Long roleId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.SECTION || role.getSectionId() == null
                || !role.getSectionId().equals(sectionId)) {
            throw new BusinessException("Role " + roleId + " does not belong to section " + sectionId);
        }

        sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not a member of section " + sectionId));

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();

        if (userRoleRepository.existsById(id)) {
            return;
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .build();
        userRoleRepository.save(userRole);
    }

    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_ASSIGN_TECH_ROLE')")
    public void removeSectionRoleFromUser(Long sectionId, Long userId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.SECTION || role.getSectionId() == null
                || !role.getSectionId().equals(sectionId)) {
            throw new BusinessException("Role " + roleId + " does not belong to section " + sectionId);
        }

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        userRoleRepository.deleteById(id);
    }
}
