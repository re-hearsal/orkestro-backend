package io.github.Romariok.orkestro.section.controller;

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
@RequestMapping("/api/v1/sections/{sectionId}/members/{userId}/roles")
public class SectionMemberTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @PostMapping("/{roleId}")
   public ResponseEntity<Void> assignSectionRoleToUser(
         @PathVariable @Positive Long sectionId,
         @PathVariable @Positive Long userId,
         @PathVariable @Positive Long roleId) {
      technicalRoleService.assignSectionRoleToUser(sectionId, userId, roleId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{roleId}")
   public ResponseEntity<Void> removeSectionRoleFromUser(
         @PathVariable @Positive Long sectionId,
         @PathVariable @Positive Long userId,
         @PathVariable @Positive Long roleId) {
      technicalRoleService.removeSectionRoleFromUser(sectionId, userId, roleId);
      return ResponseEntity.noContent().build();
   }
}

