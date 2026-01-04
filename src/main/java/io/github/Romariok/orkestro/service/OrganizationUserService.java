package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.user.UserRole;
import io.github.Romariok.orkestro.models.user.UserRoleId;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationUserService {

      private final OrganizationUserRepository organizationUserRepository;
      private final RoleRepository roleRepository;
      private final UserRoleRepository userRoleRepository;
      private final SecurityUtils securityUtils;

      @Transactional(readOnly = true)
      @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_VIEW')")
      public List<OrganizationUser> getPendingJoinRequests(Long organizationId) {
            return organizationUserRepository.findByOrganizationIdAndStatus(
                        organizationId,
                        OrganizationUserStatusType.PENDING);
      }

      @Transactional
      @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')")
      public void approveJoinRequest(Long organizationId, Long userId) {
            var organizationUser = organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                    "Join request not found for organization " + organizationId + " and user "
                                                + userId));

            if (organizationUser.getStatus() != OrganizationUserStatusType.PENDING) {
                  throw new BusinessException("Join request already processed");
            }

            organizationUser.setStatus(OrganizationUserStatusType.ACCEPTED);
            organizationUserRepository.save(organizationUser);

            long acceptedCount = organizationUserRepository.countByOrganizationIdAndStatus(
                        organizationId,
                        OrganizationUserStatusType.ACCEPTED);

            // Второму принятому участнику выдаём роль Co-leader
            if (acceptedCount == 2) {
                  Role role = roleRepository.findByScopeAndOrganizationIdAndName(
                              RoleScopeType.ORGANIZATION,
                              organizationId,
                              "Co-leader")
                              .orElse(null);

                  if (role != null) {
                        UserRoleId id = UserRoleId.builder()
                                    .userId(userId)
                                    .roleId(role.getId())
                                    .build();

                        if (!userRoleRepository.existsById(id)) {
                              UserRole userRole = UserRole.builder()
                                          .userId(userId)
                                          .roleId(role.getId())
                                          .build();
                              userRoleRepository.save(userRole);
                        }
                  }
            }
      }

      /**
       * Текущий пользователь выходит из организации.
       * Если у него есть роль Leader в этой организации — выход запрещён,
       * пока он не переназначит роль на другого пользователя.
       */
      @Transactional
      public void leaveCurrentOrganization(Long organizationId) {
            Long currentUserId = securityUtils.getCurrentUserId();

            organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, currentUserId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                    "User " + currentUserId + " is not a member of organization " + organizationId));

            // Проверяем, что пользователь не является Leader в этой организации
            roleRepository.findByScopeAndOrganizationIdAndName(
                        RoleScopeType.ORGANIZATION,
                        organizationId,
                        "Leader")
                        .ifPresent(leaderRole -> {
                              UserRoleId leaderMappingId = UserRoleId.builder()
                                          .userId(currentUserId)
                                          .roleId(leaderRole.getId())
                                          .build();

                              if (userRoleRepository.existsById(leaderMappingId)) {
                                    throw new BusinessException(
                                                "Leader must transfer the Leader role to another user before leaving the organization");
                              }
                        });

            // Удаляем все организационные роли пользователя в этой организации
            List<Role> organizationRoles = roleRepository.findByScopeAndOrganizationId(
                        RoleScopeType.ORGANIZATION,
                        organizationId);
            if (!organizationRoles.isEmpty()) {
                  List<Long> roleIds = organizationRoles.stream()
                              .map(Role::getId)
                              .toList();
                  userRoleRepository.deleteByUserIdAndRoleIdIn(currentUserId, roleIds);
            }

            // Удаляем саму связь пользователя с организацией
            organizationUserRepository.deleteByOrganizationIdAndUserId(organizationId, currentUserId);
      }

      @Transactional
      @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')")
      public void rejectJoinRequest(Long organizationId, Long userId) {
            var organizationUser = organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                    "Join request not found for organization " + organizationId + " and user "
                                                + userId));

            if (organizationUser.getStatus() != OrganizationUserStatusType.PENDING) {
                  throw new BusinessException("Join request already processed");
            }

            organizationUser.setStatus(OrganizationUserStatusType.REJECTED);
            organizationUserRepository.save(organizationUser);
      }
}
