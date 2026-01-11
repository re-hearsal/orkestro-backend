package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationUserServiceTest {

      @Mock
      private OrganizationUserRepository organizationUserRepository;

      @Mock
      private RoleRepository roleRepository;

      @Mock
      private UserRoleRepository userRoleRepository;

      @Mock
      private SecurityUtils securityUtils;

      @InjectMocks
      private OrganizationUserService organizationUserService;

      @Test
      void getPendingJoinRequests_returnsOnlyPendingForOrganization() {
            OrganizationUser ou = new OrganizationUser();
            ou.setOrganizationId(1L);
            ou.setUserId(10L);
            ou.setStatus(OrganizationUserStatusType.PENDING);
            ou.setJoinedAt(Instant.now());

            when(organizationUserRepository.findByOrganizationIdAndStatus(1L, OrganizationUserStatusType.PENDING))
                        .thenReturn(List.of(ou));

            var result = organizationUserService.getPendingJoinRequests(1L);

            assertEquals(1, result.size());
            assertEquals(OrganizationUserStatusType.PENDING, result.getFirst().getStatus());
            verify(organizationUserRepository).findByOrganizationIdAndStatus(1L, OrganizationUserStatusType.PENDING);
      }

      @Test
      void approveJoinRequest_notFound_throwsEntityNotFound() {
            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.empty());

            assertThrows(
                        EntityNotFoundException.class,
                        () -> organizationUserService.approveJoinRequest(1L, 10L));

            verify(organizationUserRepository, never()).save(any());
      }

      @Test
      void approveJoinRequest_notPending_throwsBusinessException() {
            OrganizationUser ou = new OrganizationUser();
            ou.setOrganizationId(1L);
            ou.setUserId(10L);
            ou.setStatus(OrganizationUserStatusType.ACCEPTED);

            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.of(ou));

            assertThrows(
                        BusinessException.class,
                        () -> organizationUserService.approveJoinRequest(1L, 10L));

            verify(organizationUserRepository, never()).save(any());
      }

      @Test
      void approveJoinRequest_pending_updatesStatusToAccepted() {
            OrganizationUser ou = new OrganizationUser();
            ou.setOrganizationId(1L);
            ou.setUserId(10L);
            ou.setStatus(OrganizationUserStatusType.PENDING);

            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.of(ou));
            when(organizationUserRepository.countByOrganizationIdAndStatus(1L, OrganizationUserStatusType.ACCEPTED))
                        .thenReturn(1L);

            organizationUserService.approveJoinRequest(1L, 10L);

            assertEquals(OrganizationUserStatusType.ACCEPTED, ou.getStatus());
            verify(organizationUserRepository).save(ou);
            verify(roleRepository, never()).findByScopeAndOrganizationIdAndName(
                        org.mockito.Mockito.any(), org.mockito.Mockito.anyLong(), org.mockito.Mockito.anyString());
            verify(userRoleRepository, never()).save(any(UserRole.class));
      }

      @Test
      void rejectJoinRequest_notFound_throwsEntityNotFound() {
            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.empty());

            assertThrows(
                        EntityNotFoundException.class,
                        () -> organizationUserService.rejectJoinRequest(1L, 10L));

            verify(organizationUserRepository, never()).save(any());
      }

      @Test
      void rejectJoinRequest_notPending_throwsBusinessException() {
            OrganizationUser ou = new OrganizationUser();
            ou.setOrganizationId(1L);
            ou.setUserId(10L);
            ou.setStatus(OrganizationUserStatusType.ACCEPTED);

            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.of(ou));

            assertThrows(
                        BusinessException.class,
                        () -> organizationUserService.rejectJoinRequest(1L, 10L));

            verify(organizationUserRepository, never()).save(any());
      }

      @Test
      void rejectJoinRequest_pending_updatesStatusToRejected() {
            OrganizationUser ou = new OrganizationUser();
            ou.setOrganizationId(1L);
            ou.setUserId(10L);
            ou.setStatus(OrganizationUserStatusType.PENDING);

            when(organizationUserRepository.findByOrganizationIdAndUserId(1L, 10L))
                        .thenReturn(Optional.of(ou));

            organizationUserService.rejectJoinRequest(1L, 10L);

            assertEquals(OrganizationUserStatusType.REJECTED, ou.getStatus());
            verify(organizationUserRepository).save(ou);
      }

      @Test
      void leaveCurrentOrganization_leaderCannotLeaveWithoutTransferringRole() {
            Long orgId = 1L;
            Long userId = 10L;

            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            OrganizationUser membership = new OrganizationUser();
            membership.setOrganizationId(orgId);
            membership.setUserId(userId);
            membership.setStatus(OrganizationUserStatusType.ACCEPTED);
            membership.setJoinedAt(Instant.now());

            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, userId))
                        .thenReturn(Optional.of(membership));

            Role leaderRole = Role.builder()
                        .id(100L)
                        .scope(RoleScopeType.ORGANIZATION)
                        .organizationId(orgId)
                        .name("Leader")
                        .build();
            when(roleRepository.findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION, orgId, "Leader"))
                        .thenReturn(Optional.of(leaderRole));

            when(userRoleRepository.existsById(any())).thenReturn(true);

            assertThrows(
                        BusinessException.class,
                        () -> organizationUserService.leaveCurrentOrganization(orgId));

            verify(organizationUserRepository, never()).delete(any());
      }

      @Test
      void leaveCurrentOrganization_nonLeaderMemberIsRemoved() {
            Long orgId = 1L;
            Long userId = 10L;

            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            OrganizationUser membership = new OrganizationUser();
            membership.setOrganizationId(orgId);
            membership.setUserId(userId);
            membership.setStatus(OrganizationUserStatusType.ACCEPTED);
            membership.setJoinedAt(Instant.now());

            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, userId))
                        .thenReturn(Optional.of(membership));

            when(roleRepository.findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION, orgId, "Leader"))
                        .thenReturn(Optional.empty());

            Role orgRole = Role.builder()
                        .id(200L)
                        .scope(RoleScopeType.ORGANIZATION)
                        .organizationId(orgId)
                        .name("SomeRole")
                        .build();
            when(roleRepository.findByScopeAndOrganizationId(RoleScopeType.ORGANIZATION, orgId))
                        .thenReturn(List.of(orgRole));

            organizationUserService.leaveCurrentOrganization(orgId);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> roleIdsCaptor = (ArgumentCaptor<List<Long>>) (ArgumentCaptor<?>) ArgumentCaptor
                        .forClass(List.class);
            verify(userRoleRepository).deleteByUserIdAndRoleIdIn(org.mockito.Mockito.eq(userId),
                        roleIdsCaptor.capture());
            List<Long> capturedRoleIds = roleIdsCaptor.getValue();
            org.junit.jupiter.api.Assertions.assertEquals(1, capturedRoleIds.size());
            org.junit.jupiter.api.Assertions.assertEquals(200L, capturedRoleIds.getFirst());

            verify(organizationUserRepository).deleteByOrganizationIdAndUserId(orgId, userId);
      }
}
