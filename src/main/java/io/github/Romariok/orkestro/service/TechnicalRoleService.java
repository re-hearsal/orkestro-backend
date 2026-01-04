package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.dto.role.TechnicalRoleDTO;
import io.github.Romariok.orkestro.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.user.UserRole;
import io.github.Romariok.orkestro.models.user.UserRoleId;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
    public void assignRoleToUser(Long userId, Long roleId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }

        Role role = roleRepository
                .findById(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + roleId));

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
    public void removeRoleFromUser(Long userId, Long roleId) {
        UserRoleId id = UserRoleId.builder()
                .userId(userId)
                .roleId(roleId)
                .build();
        userRoleRepository.deleteById(id);
    }
}
