package io.github.Romariok.orkestro.section.controller;

import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/sections")
public class SectionController {

   private final SectionService sectionService;

   @PostMapping("/{parentSectionId}/sections")
   public ResponseEntity<SectionDTO> createSectionInSection(
         @PathVariable @Positive Long parentSectionId,
         @Valid @RequestBody SectionCreateRequestDTO request) {
      SectionDTO created = sectionService.createSectionInSection(parentSectionId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }

   @DeleteMapping("/{sectionId}")
   public ResponseEntity<Void> deleteSection(
         @PathVariable @Positive Long sectionId) {
      sectionService.deleteSection(sectionId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{sectionId}/leave")
   public ResponseEntity<Void> leaveSection(
         @PathVariable @Positive Long sectionId) {
      sectionService.leaveCurrentSection(sectionId);
      return ResponseEntity.noContent().build();
   }
}

