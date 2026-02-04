package io.github.Romariok.orkestro.security;

import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("organizationPermissionChecker")
@RequiredArgsConstructor
public class OrganizationPermissionChecker {

   private final SecurityUtils securityUtils;
   private final UserRoleRepository userRoleRepository;
   private final RolePermissionRepository rolePermissionRepository;
   private final OrganizationUserRepository organizationUserRepository;
   private final SectionUserRepository sectionUserRepository;

   public boolean hasOrganizationPermission(Long organizationId, String permissionCode) {
      return hasPermission(RoleScopeType.ORGANIZATION, organizationId, permissionCode);
   }

   public boolean isAcceptedOrganizationMember(Long organizationId) {
      if (organizationId == null || organizationId <= 0) {
         return false;
      }
      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }
      return organizationUserRepository
            .findByOrganizationIdAndUserId(organizationId, userId)
            .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
            .isPresent();
   }

   public boolean isSectionMember(Long sectionId) {
      if (sectionId == null || sectionId <= 0) {
         return false;
      }
      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }
      return sectionUserRepository.findBySectionIdAndUserId(sectionId, userId).isPresent();
   }

   public boolean hasSectionPermission(Long sectionId, String permissionCode) {
      return hasPermission(RoleScopeType.SECTION, sectionId, permissionCode);
   }

   private boolean hasPermission(RoleScopeType scope, Long contextId, String permissionCode) {
      if (contextId == null || permissionCode == null || permissionCode.isBlank()) {
         return false;
      }
      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }

      // For section-scoped permissions, user must be a section member.
      if (scope == RoleScopeType.SECTION) {
         if (sectionUserRepository.findBySectionIdAndUserId(contextId, userId).isEmpty()) {
            return false;
         }
      }

      List<Role> roles = userRoleRepository.findRolesByUserId(userId);
      for (Role role : roles) {
         if (role.getScope() != scope) {
            continue;
         }
         if (scope == RoleScopeType.ORGANIZATION) {
            if (role.getOrganizationId() == null || !role.getOrganizationId().equals(contextId)) {
               continue;
            }
         } else if (scope == RoleScopeType.SECTION) {
            if (role.getSectionId() == null || !role.getSectionId().equals(contextId)) {
               continue;
            }
         }
         List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(role.getId());
         for (Permission permission : permissions) {
            if (permissionCode.equals(permission.getCode())) {
               return true;
            }
         }
      }
      return false;
   }
}
