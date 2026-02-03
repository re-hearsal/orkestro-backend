package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationMusicalRoleService {

   private final OrganizationUserRepository organizationUserRepository;
   private final MusicalRoleService musicalRoleService;

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_INVITE')")
   public void setMemberInstruments(Long organizationId, Long userId, List<Long> instrumentIds) {
      ensureAcceptedMember(organizationId, userId);
      musicalRoleService.setUserInstruments(userId, instrumentIds);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_INVITE')")
   public void addMemberInstrument(Long organizationId, Long userId, Long instrumentId) {
      ensureAcceptedMember(organizationId, userId);
      musicalRoleService.addInstrumentToUser(userId, instrumentId);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_INVITE')")
   public void removeMemberInstrument(Long organizationId, Long userId, Long instrumentId) {
      ensureAcceptedMember(organizationId, userId);
      musicalRoleService.removeInstrumentFromUser(userId, instrumentId);
   }

   private void ensureAcceptedMember(Long organizationId, Long userId) {
      organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId)
            .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
            .orElseThrow(() -> new BusinessException(
                  "User " + userId + " is not an accepted member of organization " + organizationId));
   }
}

