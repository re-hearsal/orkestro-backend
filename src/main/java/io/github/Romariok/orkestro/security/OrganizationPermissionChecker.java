package io.github.Romariok.orkestro.security;

import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.models.TaskAssignee;
import io.github.Romariok.orkestro.task.models.TaskVisibilityRole;
import io.github.Romariok.orkestro.task.repository.TaskAssigneeRepository;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.task.repository.TaskVisibilityRoleRepository;
import io.github.Romariok.orkestro.task.service.TaskAccessEvaluator;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("organizationPermissionChecker")
@RequiredArgsConstructor
public class OrganizationPermissionChecker {

   private final SecurityUtils securityUtils;
   private final UserRoleRepository userRoleRepository;
   private final RolePermissionRepository rolePermissionRepository;
   private final OrganizationUserRepository organizationUserRepository;
   private final OrganizationRepository organizationRepository;
   private final SectionUserRepository sectionUserRepository;
   private final SectionRepository sectionRepository;
   private final TaskRepository taskRepository;
   private final TaskVisibilityRoleRepository taskVisibilityRoleRepository;
   private final TaskAssigneeRepository taskAssigneeRepository;
   private final TaskAccessEvaluator taskAccessEvaluator;

   public boolean hasOrganizationPermission(Long organizationId, String permissionCode) {
      return hasPermission(RoleScopeType.ORGANIZATION, organizationId, permissionCode);
   }

   public boolean isAcceptedOrganizationMember(Long organizationId) {
      if (organizationId == null || organizationId <= 0) {
         return false;
      }
      if (!organizationRepository.existsById(organizationId)) {
         throw new EntityNotFoundException("Organization not found: " + organizationId);
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
      if (!sectionRepository.existsById(sectionId)) {
         throw new EntityNotFoundException("Section not found: " + sectionId);
      }
      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }
      return sectionUserRepository.findBySectionIdAndUserId(sectionId, userId).isPresent();
   }

   public boolean isAcceptedOrganizationMemberBySectionId(Long sectionId) {
      if (sectionId == null || sectionId <= 0) {
         return false;
      }

      Long organizationId = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new EntityNotFoundException("Section not found: " + sectionId))
            .getOrganizationId();

      return isAcceptedOrganizationMember(organizationId);
   }

   public boolean hasSectionPermission(Long sectionId, String permissionCode) {
      return hasPermission(RoleScopeType.SECTION, sectionId, permissionCode);
   }

   public boolean hasTaskManageOrIsAuthor(Long taskId) {
      if (taskId == null || taskId <= 0) return false;
      Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }
      if (task.getAuthorUserId().equals(userId)) return true;
      return hasOrganizationPermission(task.getOrganizationId(), "TASK_MANAGE");
   }

   public boolean hasTaskAcces(Long taskId) {
      if (taskId == null || taskId <= 0) {
         return false;
      }

      Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

      Long userId;
      try {
         userId = securityUtils.getCurrentUserId();
      } catch (SecurityException ex) {
         return false;
      }

      boolean isAcceptedMember = organizationUserRepository
            .findByOrganizationIdAndUserId(task.getOrganizationId(), userId)
            .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
            .isPresent();
      if (!isAcceptedMember) {
         return false;
      }

      Set<Long> userRoleIds = userRoleRepository.findRolesByUserId(userId).stream()
            .map(Role::getId)
            .collect(Collectors.toSet());
      if (userRoleIds.isEmpty()) {
         return false;
      }

      List<TaskVisibilityRole> allowedRoles = taskVisibilityRoleRepository.findByTaskId(taskId);
      List<Long> allowedRoleIds = allowedRoles.stream().map(TaskVisibilityRole::getRoleId).toList();
      Set<Long> assigneeUserIds = taskAssigneeRepository.findByTaskId(taskId).stream()
            .map(TaskAssignee::getUserId)
            .collect(Collectors.toSet());
      return taskAccessEvaluator.hasTaskAccess(userId, task, assigneeUserIds, userRoleIds, Map.of(taskId, allowedRoleIds));
   }

   private boolean hasPermission(RoleScopeType scope, Long contextId, String permissionCode) {
      if (contextId == null || permissionCode == null || permissionCode.isBlank()) {
         return false;
      }

      if (scope == RoleScopeType.ORGANIZATION && !organizationRepository.existsById(contextId)) {
         throw new EntityNotFoundException("Organization not found: " + contextId);
      }
      if (scope == RoleScopeType.SECTION && !sectionRepository.existsById(contextId)) {
         throw new EntityNotFoundException("Section not found: " + contextId);
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
