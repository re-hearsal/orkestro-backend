package io.github.Romariok.orkestro.security;

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

   public boolean hasOrganizationPermission(Long organizationId, String permissionCode) {
      return hasPermission(RoleScopeType.ORGANIZATION, organizationId, permissionCode);
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
