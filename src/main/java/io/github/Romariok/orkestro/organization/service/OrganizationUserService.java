package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.UserRoleId;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationUserService {

      private final OrganizationUserRepository organizationUserRepository;
      private final OrganizationRepository organizationRepository;
      private final RoleRepository roleRepository;
      private final UserRoleRepository userRoleRepository;
      private final UserRepository userRepository;
      private final SecurityUtils securityUtils;

      @Transactional
      public void requestToJoinPublicOrganization(Long organizationId) {
            var organization = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

            if (organization.getVisibilityLevel() != VisibilityLevelType.PUBLIC) {
                  throw new BusinessException("Organization " + organizationId + " is not PUBLIC");
            }

            Long currentUserId = securityUtils.getCurrentUserId();

            var existing = organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId);
            if (existing.isPresent()) {
                  OrganizationUser ou = existing.get();
                  if (ou.getStatus() == OrganizationUserStatusType.ACCEPTED
                              || ou.getStatus() == OrganizationUserStatusType.PENDING) {
                        return;
                  }
                  // REJECTED -> allow re-apply
                  ou.setStatus(OrganizationUserStatusType.PENDING);
                  ou.setJoinedAt(Instant.now());
                  organizationUserRepository.save(ou);
                  return;
            }

            OrganizationUser ou = OrganizationUser.builder()
                        .organizationId(organizationId)
                        .userId(currentUserId)
                        .status(OrganizationUserStatusType.PENDING)
                        .joinedAt(Instant.now())
                        .build();
            organizationUserRepository.save(ou);
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_INVITE')")
      public void addUserToOrganization(Long organizationId, Long userId) {
            if (userId == null || userId <= 0) {
                  throw new IllegalArgumentException("userId must be positive");
            }

            if (!organizationRepository.existsById(organizationId)) {
                  throw new EntityNotFoundException("Organization not found: " + organizationId);
            }

            if (!userRepository.existsById(userId)) {
                  throw new EntityNotFoundException("User not found: " + userId);
            }

            OrganizationUser ou = organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElse(null);

            if (ou != null && ou.getStatus() == OrganizationUserStatusType.ACCEPTED) {
                  return;
            }

            if (ou == null) {
                  ou = OrganizationUser.builder()
                              .organizationId(organizationId)
                              .userId(userId)
                              .joinedAt(Instant.now())
                              .build();
            }

            ou.setStatus(OrganizationUserStatusType.ACCEPTED);
            organizationUserRepository.save(ou);

            assignCoLeaderIfSecondAccepted(organizationId, userId);
      }

      @Transactional(readOnly = true)
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_JOIN_REQUEST_VIEW')")
      public List<OrganizationUser> getPendingJoinRequests(Long organizationId) {
            return organizationUserRepository.findByOrganizationIdAndStatus(
                        organizationId,
                        OrganizationUserStatusType.PENDING);
      }

      @Transactional(readOnly = true)
      @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
      public Page<OrganizationMemberDTO> searchMembers(
                  Long organizationId,
                  String query,
                  List<Long> roleIds,
                  List<Long> instrumentIds,
                  Pageable pageable) {
            if (!organizationRepository.existsById(organizationId)) {
                  throw new EntityNotFoundException("Organization not found: " + organizationId);
            }

            String normalized = query == null ? null : query.trim();
            if (normalized != null && normalized.isBlank()) {
                  normalized = null;
            }

            boolean applyRoleFilter = roleIds != null && !roleIds.isEmpty();
            List<Long> roleIdsParam = applyRoleFilter ? roleIds : List.of(-1L);

            boolean applyInstrumentFilter = instrumentIds != null && !instrumentIds.isEmpty();
            List<Long> instrumentIdsParam = applyInstrumentFilter ? instrumentIds : List.of(-1L);

            Page<User> users = userRepository.searchOrganizationMembers(
                        organizationId,
                        normalized,
                        applyRoleFilter,
                        roleIdsParam,
                        applyInstrumentFilter,
                        instrumentIdsParam,
                        OrganizationUserStatusType.ACCEPTED,
                        RoleScopeType.ORGANIZATION,
                        pageable);

            return users.map(u -> new OrganizationMemberDTO(
                        u.getId(),
                        u.getUsername(),
                        u.getName(),
                        u.getProfileImageFileId()));
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_JOIN_REQUEST_MANAGE')")
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

            assignCoLeaderIfSecondAccepted(organizationId, userId);
      }

      @Transactional
      public void leaveCurrentOrganization(Long organizationId) {
            Long currentUserId = securityUtils.getCurrentUserId();
            removeUserFromOrganizationInternal(organizationId, currentUserId);
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_REMOVE')")
      public void removeUserFromOrganization(Long organizationId, Long userId) {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId != null && currentUserId.equals(userId)) {
                  throw new IllegalArgumentException(
                              "Cannot remove yourself from organization using this endpoint. Use leave endpoint instead.");
            }
            removeUserFromOrganizationInternal(organizationId, userId);
      }

      @Transactional(propagation = Propagation.SUPPORTS)
      public void removeUserFromOrganizationInternal(Long organizationId, Long currentUserId) {
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

            organizationUserRepository.deleteByOrganizationIdAndUserId(organizationId, currentUserId);
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_JOIN_REQUEST_MANAGE')")
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

      private void assignCoLeaderIfSecondAccepted(Long organizationId, Long userId) {
            long acceptedCount = organizationUserRepository.countByOrganizationIdAndStatus(
                        organizationId,
                        OrganizationUserStatusType.ACCEPTED);

            if (acceptedCount != 2) {
                  return;
            }

            Role role = roleRepository.findByScopeAndOrganizationIdAndName(
                        RoleScopeType.ORGANIZATION,
                        organizationId,
                        "Co-leader")
                        .orElse(null);

            if (role == null) {
                  return;
            }

            UserRoleId id = UserRoleId.builder()
                        .userId(userId)
                        .roleId(role.getId())
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
}
