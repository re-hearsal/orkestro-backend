package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.section.dto.SectionMemberDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/sections/{sectionId}/members")
public class SectionMemberController {

   private final SectionService sectionService;

   @PostMapping("/{userId}")
   public ResponseEntity<Void> addUserToSection(
         @PathVariable @Positive Long sectionId,
         @PathVariable @Positive Long userId) {
      sectionService.addUserToSection(sectionId, userId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{userId}")
   public ResponseEntity<Void> removeUserFromSection(
         @PathVariable @Positive Long sectionId,
         @PathVariable @Positive Long userId) {
      sectionService.removeUserFromSection(sectionId, userId);
      return ResponseEntity.noContent().build();
   }

   @GetMapping("/page")
   public ResponseEntity<Page<SectionMemberDTO>> searchMembersPage(
         @PathVariable @Positive Long sectionId,
         @RequestParam(required = false) String query,
         @RequestParam(required = false) List<@Positive Long> roleIds,
         @RequestParam(required = false) List<@Positive Long> instrumentIds,
         @PageableDefault(size = 20) Pageable pageable) {
      return ResponseEntity.ok(
            sectionService.searchMembers(
                  sectionId,
                  query,
                  roleIds,
                  instrumentIds,
                  pageable));
   }
}

