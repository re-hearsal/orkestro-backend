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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/sections")
public class OrganizationSectionController {

   private final SectionService sectionService;

   @PostMapping
   public ResponseEntity<SectionDTO> createSectionInOrganization(
         @PathVariable @Positive Long organizationId,
         @Valid @RequestBody SectionCreateRequestDTO request) {
      SectionDTO created = sectionService.createSectionInOrganization(organizationId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }
}

