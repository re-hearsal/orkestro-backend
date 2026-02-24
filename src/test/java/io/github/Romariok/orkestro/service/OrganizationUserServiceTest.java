package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.organization.service.OrganizationService;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.organization.mapper.OrganizationMemberMapper;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
      private OrganizationRepository organizationRepository;

      @Mock
      private RoleRepository roleRepository;

      @Mock
      private UserRoleRepository userRoleRepository;

      @Mock
      private UserRepository userRepository;

      @Mock
      private SecurityUtils securityUtils;

      @Mock
      private OrganizationMemberMapper organizationMemberMapper;

      @Mock
      private OrganizationService organizationService;

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

            organizationUserService.approveJoinRequest(1L, 10L);

            assertEquals(OrganizationUserStatusType.ACCEPTED, ou.getStatus());
            verify(organizationUserRepository).save(ou);
            verify(organizationService).syncOrganizationLeadershipRoles(1L);
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

            OrganizationUser otherMember = new OrganizationUser();
            otherMember.setOrganizationId(orgId);
            otherMember.setUserId(20L);
            otherMember.setStatus(OrganizationUserStatusType.ACCEPTED);
            otherMember.setJoinedAt(Instant.now());

            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, userId))
                        .thenReturn(Optional.of(membership));
            when(organizationUserRepository.findByOrganizationIdAndStatusOrderByJoinedAtAsc(
                        orgId, OrganizationUserStatusType.ACCEPTED))
                        .thenReturn(List.of(membership, otherMember));

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

            verify(organizationUserRepository, never()).deleteByOrganizationIdAndUserId(
                        org.mockito.Mockito.anyLong(),
                        org.mockito.Mockito.anyLong());
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
            when(organizationUserRepository.findByOrganizationIdAndStatusOrderByJoinedAtAsc(
                        orgId, OrganizationUserStatusType.ACCEPTED))
                        .thenReturn(List.of(membership));
            when(organizationUserRepository.countByOrganizationIdAndStatus(orgId, OrganizationUserStatusType.ACCEPTED))
                        .thenReturn(0L);

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
            verify(organizationService).deleteOrganizationCascade(orgId);
      }

      @Test
      void removeUserFromOrganization_cannotRemoveSelf_throwsIllegalArgumentException() {
            Long orgId = 1L;
            Long userId = 10L;

            when(securityUtils.getCurrentUserId()).thenReturn(userId);

            assertThrows(
                        IllegalArgumentException.class,
                        () -> organizationUserService.removeUserFromOrganization(orgId, userId));

            verify(organizationUserRepository, never()).findByOrganizationIdAndUserId(any(), any());
            verify(organizationUserRepository, never()).deleteByOrganizationIdAndUserId(any(), any());
      }

      @Test
      void requestToJoinPublicOrganization_notFound_throwsEntityNotFound() {
            when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                        () -> organizationUserService.requestToJoinPublicOrganization(1L, "test description"));
      }

      @Test
      void requestToJoinPublicOrganization_privateOrg_throwsBusinessException() {
            Organization org = Organization.builder()
                        .id(1L)
                        .visibilityLevel(VisibilityLevelType.PRIVATE)
                        .build();
            when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

            assertThrows(BusinessException.class,
                        () -> organizationUserService.requestToJoinPublicOrganization(1L, "test description"));
      }

      @Test
      void requestToJoinPublicOrganization_newRequest_savesPending() {
            Long orgId = 1L;
            Long currentUserId = 10L;

            Organization org = Organization.builder()
                        .id(orgId)
                        .visibilityLevel(VisibilityLevelType.PUBLIC)
                        .build();
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, currentUserId))
                        .thenReturn(Optional.empty());

            String description = "Join request description";
            organizationUserService.requestToJoinPublicOrganization(orgId, description);

            ArgumentCaptor<OrganizationUser> captor = ArgumentCaptor.forClass(OrganizationUser.class);
            verify(organizationUserRepository).save(captor.capture());
            OrganizationUser saved = captor.getValue();
            assertEquals(orgId, saved.getOrganizationId());
            assertEquals(currentUserId, saved.getUserId());
            assertEquals(OrganizationUserStatusType.PENDING, saved.getStatus());
            assertEquals(description, saved.getDescription());
      }

      @Test
      void requestToJoinPublicOrganization_alreadyPending_isIdempotent() {
            Long orgId = 1L;
            Long currentUserId = 10L;

            Organization org = Organization.builder()
                        .id(orgId)
                        .visibilityLevel(VisibilityLevelType.PUBLIC)
                        .build();
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

            OrganizationUser existing = OrganizationUser.builder()
                        .organizationId(orgId)
                        .userId(currentUserId)
                        .status(OrganizationUserStatusType.PENDING)
                        .build();
            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, currentUserId))
                        .thenReturn(Optional.of(existing));

            organizationUserService.requestToJoinPublicOrganization(orgId, "Join request description");

            verify(organizationUserRepository, never()).save(any());
      }

      @Test
      void addUserToOrganization_newMember_savesAcceptedAndAssignsCoLeaderIfSecondAccepted() {
            Long orgId = 1L;
            Long userId = 10L;

            when(organizationRepository.existsById(orgId)).thenReturn(true);
            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationUserRepository.findByOrganizationIdAndUserId(orgId, userId))
                        .thenReturn(Optional.empty());

            organizationUserService.addUserToOrganization(orgId, userId);

            ArgumentCaptor<OrganizationUser> ouCaptor = ArgumentCaptor.forClass(OrganizationUser.class);
            verify(organizationUserRepository).save(ouCaptor.capture());
            assertEquals(OrganizationUserStatusType.ACCEPTED, ouCaptor.getValue().getStatus());

            verify(organizationService).syncOrganizationLeadershipRoles(orgId);
      }

      @Test
      void searchMembers_normalizesQueryAndMapsToDto() {
            Long orgId = 1L;
            when(organizationRepository.existsById(orgId)).thenReturn(true);

            User user = User.builder()
                        .id(10L)
                        .username("u")
                        .name("User")
                        .profileImageFileId(123L)
                        .build();

            OrganizationUser ou = OrganizationUser.builder()
                        .organizationId(orgId)
                        .userId(10L)
                        .status(OrganizationUserStatusType.ACCEPTED)
                        .joinedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build();
            ou.setUser(user);

            Page<OrganizationUser> page = new PageImpl<>(List.of(ou), PageRequest.of(0, 20), 1);
            when(organizationUserRepository.findAll(
                        org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<OrganizationUser>>any(),
                        org.mockito.Mockito.any(org.springframework.data.domain.Pageable.class)))
                        .thenReturn(page);
            when(organizationMemberMapper.toDto(
                        org.mockito.Mockito.any(User.class),
                        org.mockito.Mockito.any(Instant.class)))
                        .thenAnswer(inv -> {
                              User u = inv.getArgument(0);
                              Instant joinedAt = inv.getArgument(1);
                              return new OrganizationMemberDTO(
                                          u.getId(),
                                          u.getUsername(),
                                          u.getName(),
                                          u.getProfileImageFileId(),
                                          joinedAt);
                        });

            Page<OrganizationMemberDTO> result = organizationUserService.searchMembers(
                        orgId,
                        "  abc ",
                        List.of(),
                        List.of(),
                        PageRequest.of(0, 20));

            assertEquals(1, result.getTotalElements());
            assertEquals(10L, result.getContent().getFirst().getId());
            assertEquals("u", result.getContent().getFirst().getUsername());
            assertEquals("User", result.getContent().getFirst().getName());
            assertEquals(123L, result.getContent().getFirst().getProfileImageFileId());
            assertEquals(Instant.parse("2026-01-01T00:00:00Z"), result.getContent().getFirst().getJoinedAt());
      }

      @Test
      void searchMembers_blankQuery_returnsAllAndKeepsOtherFilters() {
            Long orgId = 1L;
            when(organizationRepository.existsById(orgId)).thenReturn(true);

            User user = User.builder()
                        .id(10L)
                        .username("u")
                        .name("User")
                        .profileImageFileId(123L)
                        .build();

            OrganizationUser ou = OrganizationUser.builder()
                        .organizationId(orgId)
                        .userId(10L)
                        .status(OrganizationUserStatusType.ACCEPTED)
                        .joinedAt(Instant.parse("2026-01-01T00:00:00Z"))
                        .build();
            ou.setUser(user);

            Page<OrganizationUser> page = new PageImpl<>(List.of(ou), PageRequest.of(0, 20), 1);
            when(organizationUserRepository.findAll(
                        org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<OrganizationUser>>any(),
                        org.mockito.Mockito.any(org.springframework.data.domain.Pageable.class)))
                        .thenReturn(page);
            when(organizationMemberMapper.toDto(
                        org.mockito.Mockito.any(User.class),
                        org.mockito.Mockito.any(Instant.class)))
                        .thenAnswer(inv -> {
                              User u = inv.getArgument(0);
                              Instant joinedAt = inv.getArgument(1);
                              return new OrganizationMemberDTO(
                                          u.getId(),
                                          u.getUsername(),
                                          u.getName(),
                                          u.getProfileImageFileId(),
                                          joinedAt);
                        });

            Page<OrganizationMemberDTO> result = organizationUserService.searchMembers(
                        orgId,
                        "   ",
                        List.of(1L),
                        List.of(),
                        PageRequest.of(0, 20));

            assertEquals(1, result.getTotalElements());
      }

      @Test
      void searchMembers_sortByJoinedAt_mapsSortToMembership() {
            Long orgId = 1L;
            when(organizationRepository.existsById(orgId)).thenReturn(true);

            when(organizationUserRepository.findAll(
                        org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<OrganizationUser>>any(),
                        org.mockito.Mockito.any(org.springframework.data.domain.Pageable.class)))
                        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            organizationUserService.searchMembers(
                        orgId,
                        null,
                        List.of(),
                        List.of(),
                        PageRequest.of(0, 20, Sort.by(Sort.Order.desc("joinedAt"))));

            ArgumentCaptor<org.springframework.data.domain.Pageable> captor = ArgumentCaptor
                        .forClass(org.springframework.data.domain.Pageable.class);
            verify(organizationUserRepository).findAll(
                        org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<OrganizationUser>>any(),
                        captor.capture());

            Sort.Order order = captor.getValue().getSort().getOrderFor("joinedAt");
            org.junit.jupiter.api.Assertions.assertNotNull(order);
            org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, order.getDirection());
      }
}
