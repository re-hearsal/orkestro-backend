package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationUserServiceTest {

   @Mock
   private OrganizationUserRepository organizationUserRepository;

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
}
