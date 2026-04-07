package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.mapper.OrganizationMemberMapper;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.organization.specification.OrganizationUserSpecifications;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.UserRoleId;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
      private final OrganizationService organizationService;
      private final RoleRepository roleRepository;
      private final UserRoleRepository userRoleRepository;
      private final SecurityUtils securityUtils;
      private final OrganizationMemberMapper organizationMemberMapper;

      @Transactional
      public void requestToJoinOrganization(Long organizationId, String description) {
            organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

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
                  ou.setDescription(description);
                  organizationUserRepository.save(ou);
                  return;
            }

            OrganizationUser ou = OrganizationUser.builder()
                        .organizationId(organizationId)
                        .userId(currentUserId)
                        .status(OrganizationUserStatusType.PENDING)
                        .joinedAt(Instant.now())
                        .description(description)
                        .build();
            organizationUserRepository.save(ou);
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

            Pageable mappedPageable = mapOrganizationMemberSort(pageable);

            Specification<OrganizationUser> spec = Specification.where(
                        OrganizationUserSpecifications.isOrganizationMember(organizationId,
                                    OrganizationUserStatusType.ACCEPTED))
                        .and(OrganizationUserSpecifications.userNameOrUsernameContainsIgnoreCase(query))
                        .and(OrganizationUserSpecifications.userHasAnyOrganizationRole(organizationId, roleIds))
                        .and(OrganizationUserSpecifications.userHasAnyInstrument(instrumentIds));

            Page<OrganizationUser> memberships = organizationUserRepository.findAll(spec, mappedPageable);
            return memberships.map(ou -> {
                  return organizationMemberMapper.toDto(ou.getUser(), ou.getJoinedAt());
            });
      }

      private Pageable mapOrganizationMemberSort(Pageable pageable) {
            if (pageable == null) {
                  return PageRequest.of(0, 20);
            }
            Sort sort = pageable.getSort();
            if (sort == null || sort.isUnsorted()) {
                  return pageable;
            }

            List<Sort.Order> mapped = new ArrayList<>();
            for (Sort.Order order : sort) {
                  String prop = order.getProperty();
                  String mappedProp = switch (prop) {
                        case "joinedAt" -> "joinedAt";
                        case "id" -> "user.id";
                        case "username" -> "user.username";
                        case "name" -> "user.name";
                        default -> throw new IllegalArgumentException("Unsupported sort property: " + prop);
                  };
                  Sort.Order mappedOrder = new Sort.Order(order.getDirection(), mappedProp)
                              .with(order.getNullHandling());
                  if (order.isIgnoreCase()) {
                        mappedOrder = mappedOrder.ignoreCase();
                  }
                  mapped.add(mappedOrder);
            }

            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mapped));
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

            organizationService.syncOrganizationLeadershipRoles(organizationId);
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
      public void leaveCurrentOrganization(Long organizationId) {
            Long currentUserId = securityUtils.getCurrentUserId();
            validateLeaveOrganization(organizationId, currentUserId);
            removeUserAndReconcileOrganization(organizationId, currentUserId);
      }

      @Transactional
      @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_REMOVE')")
      public void removeUserFromOrganization(Long organizationId, Long userId) {
            Long currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId != null && currentUserId.equals(userId)) {
                  throw new IllegalArgumentException(
                              "Cannot remove yourself from organization using this endpoint. Use leave endpoint instead.");
            }
            removeUserAndReconcileOrganization(organizationId, userId);
      }

      private void validateLeaveOrganization(Long organizationId, Long userId) {
            organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                    "User " + userId + " is not a member of organization " + organizationId));

            List<OrganizationUser> members = organizationUserRepository
                        .findByOrganizationIdAndStatusOrderByJoinedAtAsc(organizationId,
                                    OrganizationUserStatusType.ACCEPTED);
            int memberCount = members.size();

            if (memberCount > 0) {
                  boolean isLeader = roleRepository.findByScopeAndOrganizationIdAndName(
                              RoleScopeType.ORGANIZATION, organizationId, "Leader")
                              .map(role -> userRoleRepository.existsById(
                                          UserRoleId.builder().userId(userId).roleId(role.getId()).build()))
                              .orElse(false);

                  boolean isCoLeader = roleRepository
                              .findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION, organizationId,
                                          "Co-leader")
                              .map(role -> userRoleRepository.existsById(
                                          UserRoleId.builder().userId(userId).roleId(role.getId()).build()))
                              .orElse(false);

                  if (isLeader && memberCount > 1) {
                        throw new BusinessException(
                                    "Leader cannot leave organization while other members exist");
                  }
                  if (isCoLeader && memberCount > 2) {
                        throw new BusinessException(
                                    "Co-leader cannot leave organization while other non-leader members exist");
                  }
            }
      }

      @Transactional(propagation = Propagation.SUPPORTS)
      public void removeUserAndReconcileOrganization(Long organizationId, Long userId) {
            organizationUserRepository
                        .findByOrganizationIdAndUserId(organizationId, userId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                    "User " + userId + " is not a member of organization " + organizationId));

            List<Role> organizationRoles = roleRepository.findByScopeAndOrganizationId(
                        RoleScopeType.ORGANIZATION,
                        organizationId);
            if (!organizationRoles.isEmpty()) {
                  List<Long> roleIds = organizationRoles.stream()
                              .map(Role::getId)
                              .toList();
                  userRoleRepository.deleteByUserIdAndRoleIdIn(userId, roleIds);
            }

            organizationUserRepository.deleteByOrganizationIdAndUserId(organizationId, userId);

            long remainingCount = organizationUserRepository.countByOrganizationIdAndStatus(
                        organizationId, OrganizationUserStatusType.ACCEPTED);
            if (remainingCount == 0) {
                  organizationService.deleteOrganizationCascade(organizationId);
            } else {
                  organizationService.syncOrganizationLeadershipRoles(organizationId);
            }
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
}
