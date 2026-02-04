package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.user.dto.TechnicalRoleCreateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/sections/{sectionId}/roles")
public class SectionTechnicalRoleController {

   private final TechnicalRoleService technicalRoleService;

   @GetMapping
   public ResponseEntity<List<TechnicalRoleDTO>> getSectionRoles(
         @PathVariable @Positive Long sectionId) {
      return ResponseEntity.ok(technicalRoleService.getSectionRoles(sectionId));
   }

   @PostMapping
   public ResponseEntity<TechnicalRoleDTO> createSectionRole(
         @PathVariable @Positive Long sectionId,
         @Valid @RequestBody TechnicalRoleCreateRequestDTO request) {
      TechnicalRoleDTO created = technicalRoleService.createSectionRole(sectionId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }
}

