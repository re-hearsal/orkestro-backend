package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.service.OrgNotificationService;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.user.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleCreateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.RolePermission;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.UserRoleId;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.PermissionRepository;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationRepository organizationRepository;
    private final SectionUserRepository sectionUserRepository;
    private final OrgNotificationService orgNotificationService;

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getUserRoles(Long userId) {
        List<Role> roles = technicalRoleDao.findUserRoles(userId);
        return roles.stream().map(this::toDtoWithPermissions).toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getOrganizationRoles(Long organizationId) {
        List<Role> roles = technicalRoleDao.findOrganizationRoles(organizationId);
        return roles.stream().map(this::toDtoWithPermissions).toList();
    }

    @Transactional(readOnly = true)
    public List<TechnicalRoleDTO> getSectionRoles(Long sectionId) {
        List<Role> roles = technicalRoleDao.findSectionRoles(sectionId);
        return roles.stream().map(this::toDtoWithPermissions).toList();
    }

    private TechnicalRoleDTO toDtoWithPermissions(Role role) {
        TechnicalRoleDTO dto = technicalRoleMapper.toDto(role);
        List<String> codes = rolePermissionRepository.findPermissionsByRoleId(role.getId())
                .stream().map(p -> p.getCode()).toList();
        dto.setPermissionCodes(codes);
        return dto;
    }


    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_TECH_ROLE_MANAGE')")
    public TechnicalRoleDTO createOrganizationRole(Long organizationId, TechnicalRoleCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }

        String normalizedName = request.getName().trim();

        roleRepository.findByScopeAndOrganizationIdAndName(
                RoleScopeType.ORGANIZATION,
                organizationId,
                normalizedName)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Role with name '" + normalizedName + "' already exists in organization " + organizationId);
                });

        Role role = Role.builder()
                .scope(RoleScopeType.ORGANIZATION)
                .organizationId(organizationId)
                .name(normalizedName)
                .system(false)
                .createdAt(Instant.now())
                .build();

        Role saved = roleRepository.save(role);

        List<String> codes = request.getPermissionCodes();
        if (codes != null && !codes.isEmpty()) {
            Set<String> uniqueCodes = new HashSet<>(codes);
            if (!uniqueCodes.isEmpty()) {
                List<Permission> permissions = permissionRepository.findByCodeIn(uniqueCodes);
                if (permissions.size() != uniqueCodes.size()) {
                    throw new EntityNotFoundException("One or more permissions not found for codes: " + uniqueCodes);
                }

                List<RolePermission> mappings = uniqueCodes.stream()
                        .map(code -> {
                            RolePermission rp = new RolePermission();
                            rp.setRoleId(saved.getId());
                            rp.setPermissionCode(code);
                            return rp;
                        })
                        .toList();
                rolePermissionRepository.saveAll(mappings);
            }
        }

        return toDtoWithPermissions(saved);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_TECH_ROLE_MANAGE')")
    public TechnicalRoleDTO updateOrganizationRole(Long organizationId, Long roleId, TechnicalRoleCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.ORGANIZATION || role.getOrganizationId() == null
                || !role.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Role " + roleId + " does not belong to organization " + organizationId);
        }

        if (role.isSystem()) {
            throw new BusinessException("System role cannot be modified");
        }

        String normalizedName = request.getName().trim();
        if (!normalizedName.equals(role.getName())) {
            roleRepository.findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION, organizationId, normalizedName)
                    .ifPresent(existing -> {
                        throw new BusinessException(
                                "Role with name '" + normalizedName + "' already exists in organization " + organizationId);
                    });
            role.setName(normalizedName);
        }

        rolePermissionRepository.deleteByRoleId(roleId);

        List<String> codes = request.getPermissionCodes();
        if (codes != null && !codes.isEmpty()) {
            Set<String> uniqueCodes = new HashSet<>(codes);
            List<Permission> permissions = permissionRepository.findByCodeIn(uniqueCodes);
            if (permissions.size() != uniqueCodes.size()) {
                throw new EntityNotFoundException("One or more permissions not found for codes: " + uniqueCodes);
            }
            List<RolePermission> mappings = uniqueCodes.stream()
                    .map(code -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionCode(code);
                        return rp;
                    })
                    .toList();
            rolePermissionRepository.saveAll(mappings);
        }

        return toDtoWithPermissions(role);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_TECH_ROLE_MANAGE')")
    public TechnicalRoleDTO createSectionRole(Long sectionId, TechnicalRoleCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }

        String normalizedName = request.getName().trim();

        roleRepository.findByScopeAndSectionIdAndName(
                RoleScopeType.SECTION,
                sectionId,
                normalizedName)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "Role with name '" + normalizedName + "' already exists in section " + sectionId);
                });

        Role role = Role.builder()
                .scope(RoleScopeType.SECTION)
                .sectionId(sectionId)
                .name(normalizedName)
                .system(false)
                .createdAt(Instant.now())
                .build();

        Role saved = roleRepository.save(role);

        List<String> codes = request.getPermissionCodes();
        if (codes != null && !codes.isEmpty()) {
            Set<String> uniqueCodes = new HashSet<>(codes);
            if (!uniqueCodes.isEmpty()) {
                List<Permission> permissions = permissionRepository.findByCodeIn(uniqueCodes);
                if (permissions.size() != uniqueCodes.size()) {
                    throw new EntityNotFoundException("One or more permissions not found for codes: " + uniqueCodes);
                }

                List<RolePermission> mappings = uniqueCodes.stream()
                        .map(code -> {
                            RolePermission rp = new RolePermission();
                            rp.setRoleId(saved.getId());
                            rp.setPermissionCode(code);
                            return rp;
                        })
                        .toList();
                rolePermissionRepository.saveAll(mappings);
            }
        }

        return toDtoWithPermissions(saved);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_TECH_ROLE_MANAGE')")
    public TechnicalRoleDTO updateSectionRole(Long sectionId, Long roleId, TechnicalRoleCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.SECTION || role.getSectionId() == null
                || !role.getSectionId().equals(sectionId)) {
            throw new BusinessException("Role " + roleId + " does not belong to section " + sectionId);
        }

        if (role.isSystem()) {
            throw new BusinessException("System role cannot be modified");
        }

        String normalizedName = request.getName().trim();
        if (!normalizedName.equals(role.getName())) {
            roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, normalizedName)
                    .ifPresent(existing -> {
                        throw new BusinessException(
                                "Role with name '" + normalizedName + "' already exists in section " + sectionId);
                    });
            role.setName(normalizedName);
        }

        rolePermissionRepository.deleteByRoleId(roleId);

        List<String> codes = request.getPermissionCodes();
        if (codes != null && !codes.isEmpty()) {
            Set<String> uniqueCodes = new HashSet<>(codes);
            List<Permission> permissions = permissionRepository.findByCodeIn(uniqueCodes);
            if (permissions.size() != uniqueCodes.size()) {
                throw new EntityNotFoundException("One or more permissions not found for codes: " + uniqueCodes);
            }
            List<RolePermission> mappings = uniqueCodes.stream()
                    .map(code -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionCode(code);
                        return rp;
                    })
                    .toList();
            rolePermissionRepository.saveAll(mappings);
        }

        return toDtoWithPermissions(role);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_TECH_ROLE_MANAGE')")
    public void deleteOrganizationRole(Long organizationId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.ORGANIZATION || role.getOrganizationId() == null
                || !role.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Role " + roleId + " does not belong to organization " + organizationId);
        }

        if (role.isSystem()) {
            throw new BusinessException("System role cannot be deleted");
        }

        if (userRoleRepository.existsByRoleId(roleId)) {
            throw new BusinessException(
                    "Role is assigned to users. Remove this role from all users before deleting it");
        }

        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_TECH_ROLE_MANAGE')")
    public void deleteSectionRole(Long sectionId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.SECTION || role.getSectionId() == null
                || !role.getSectionId().equals(sectionId)) {
            throw new BusinessException("Role " + roleId + " does not belong to section " + sectionId);
        }

        if (role.isSystem()) {
            throw new BusinessException("System role cannot be deleted");
        }

        if (userRoleRepository.existsByRoleId(roleId)) {
            throw new BusinessException(
                    "Role is assigned to users. Remove this role from all users before deleting it");
        }

        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_ASSIGN_TECH_ROLE')")
    public void assignOrganizationRoleToUser(Long organizationId, Long userId, Long roleId) {
        assignOrganizationRoleToUserInternal(organizationId, userId, roleId);
        Role role = roleRepository.findById(roleId).orElse(null);
        String roleName = role != null ? role.getName() : "";
        String orgName = organizationRepository.findById(organizationId).map(Organization::getName).orElse("");
        orgNotificationService.notifyRoleAssigned(organizationId, userId, roleName, orgName);
    }


    @Transactional
    public void assignOrganizationRoleToUserInternal(Long organizationId, Long userId, Long roleId) {
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
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_ASSIGN_TECH_ROLE')")
    public void removeOrganizationRoleFromUser(Long organizationId, Long userId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.ORGANIZATION || role.getOrganizationId() == null
                || !role.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Role " + roleId + " does not belong to organization " + organizationId);
        }

        if (role.isSystem()) {
            if ("Leader".equals(role.getName())) {
                long leaderCount = userRoleRepository.findByRoleId(roleId).size();
                if (leaderCount <= 1) {
                    throw new BusinessException("Cannot remove the last leader of the organization");
                }
            } else if ("Co-leader".equals(role.getName())) {
                long coLeaderCount = userRoleRepository.findByRoleId(roleId).size();
                if (coLeaderCount <= 1) {
                    throw new BusinessException("Cannot remove the last co-leader of the organization");
                }
            }
        }

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        userRoleRepository.deleteById(id);

        String orgName = organizationRepository.findById(organizationId).map(Organization::getName).orElse("");
        orgNotificationService.notifyRoleRemoved(organizationId, userId, role.getName(), orgName);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_ASSIGN_TECH_ROLE')")
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
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_ASSIGN_TECH_ROLE')")
    public void removeSectionRoleFromUser(Long sectionId, Long userId, Long roleId) {
        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

        if (role.getScope() != RoleScopeType.SECTION || role.getSectionId() == null
                || !role.getSectionId().equals(sectionId)) {
            throw new BusinessException("Role " + roleId + " does not belong to section " + sectionId);
        }

        if (role.isSystem()) {
            if ("Leader".equals(role.getName())) {
                long leaderCount = userRoleRepository.findByRoleId(roleId).size();
                if (leaderCount <= 1) {
                    throw new BusinessException("Cannot remove the last leader of the section");
                }
            } else if ("Co-leader".equals(role.getName())) {
                long coLeaderCount = userRoleRepository.findByRoleId(roleId).size();
                if (coLeaderCount <= 1) {
                    throw new BusinessException("Cannot remove the last co-leader of the section");
                }
            }
        }

        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        userRoleRepository.deleteById(id);
    }
}
