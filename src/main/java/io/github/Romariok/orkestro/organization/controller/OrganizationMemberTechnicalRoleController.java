package io.github.Romariok.orkestro.organization.controller;

import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/members/{userId}/roles")
public class OrganizationMemberTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @PostMapping("/{roleId}")
   public ResponseEntity<Void> assignOrganizationRoleToUser(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long userId,
         @PathVariable @Positive Long roleId) {
      technicalRoleService.assignOrganizationRoleToUser(organizationId, userId, roleId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{roleId}")
   public ResponseEntity<Void> removeOrganizationRoleFromUser(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long userId,
         @PathVariable @Positive Long roleId) {
      technicalRoleService.removeOrganizationRoleFromUser(organizationId, userId, roleId);
      return ResponseEntity.noContent().build();
   }
}

