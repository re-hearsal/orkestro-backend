package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.models.OrganizationUser;
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
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

   @Transactional(readOnly = true)
   @PreAuthorize("hasAuthority('ORG_JOIN_REQUEST_VIEW') or " +
         "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_VIEW')")
   public List<OrganizationUser> getPendingJoinRequests(Long organizationId) {
      return organizationUserRepository.findByOrganizationIdAndStatus(
            organizationId,
            OrganizationUserStatusType.PENDING);
   }

   @Transactional
   @PreAuthorize("hasAuthority('ORG_JOIN_REQUEST_MANAGE') or " +
         "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')")
   public void approveJoinRequest(Long organizationId, Long userId) {
      var organizationUser = organizationUserRepository
            .findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new EntityNotFoundException(
                  "Join request not found for organization " + organizationId + " and user " + userId));

      if (organizationUser.getStatus() != OrganizationUserStatusType.PENDING) {
         throw new BusinessException("Join request already processed");
      }

      organizationUser.setStatus(OrganizationUserStatusType.ACCEPTED);
      organizationUserRepository.save(organizationUser);
   }


   @Transactional
   @PreAuthorize("hasAuthority('ORG_JOIN_REQUEST_MANAGE') or " +
         "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')")
   public void rejectJoinRequest(Long organizationId, Long userId) {
      var organizationUser = organizationUserRepository
            .findByOrganizationIdAndUserId(organizationId, userId)
            .orElseThrow(() -> new EntityNotFoundException(
                  "Join request not found for organization " + organizationId + " and user " + userId));

      if (organizationUser.getStatus() != OrganizationUserStatusType.PENDING) {
         throw new BusinessException("Join request already processed");
      }

      organizationUser.setStatus(OrganizationUserStatusType.REJECTED);
      organizationUserRepository.save(organizationUser);
   }
}
