package io.github.Romariok.orkestro.organization.controller;

import io.github.Romariok.orkestro.organization.dto.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberAddRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationMemberDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationJoinRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationVisibilityUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.service.OrganizationService;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

   private final OrganizationService organizationService;
   private final OrganizationUserService organizationUserService;

   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<OrganizationDTO> createOrganization(
         @Valid @ModelAttribute OrganizationCreateRequestDTO request) {
      OrganizationDTO created = organizationService.createOrganization(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }

   @GetMapping("/{organizationId}")
   public ResponseEntity<OrganizationDTO> getOrganization(
         @PathVariable @Positive Long organizationId) {
      return ResponseEntity.ok(organizationService.getOrganization(organizationId));
   }

   @PatchMapping("/{organizationId}")
   public ResponseEntity<OrganizationDTO> updateOrganization(
         @PathVariable @Positive Long organizationId,
         @Valid @RequestBody OrganizationUpdateRequestDTO request) {
      return ResponseEntity.ok(organizationService.updateOrganization(organizationId, request));
   }

   @DeleteMapping("/{organizationId}/profile-image")
   public ResponseEntity<Void> deleteOrganizationProfileImage(
         @PathVariable @Positive Long organizationId) {
      organizationService.deleteOrganizationProfileImage(organizationId);
      return ResponseEntity.noContent().build();
   }

   @PutMapping("/{organizationId}/visibility")
   public ResponseEntity<OrganizationDTO> updateVisibility(
         @PathVariable @Positive Long organizationId,
         @Valid @RequestBody OrganizationVisibilityUpdateRequestDTO request) {
      return ResponseEntity.ok(organizationService.setVisibility(
            organizationId, request.getVisibilityLevel()));
   }

   @DeleteMapping("/{organizationId}")
   public ResponseEntity<Void> deleteOrganization(
         @PathVariable @Positive Long organizationId) {
      organizationService.deleteOrganization(organizationId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{organizationId}/leave")
   public ResponseEntity<Void> leaveOrganization(
         @PathVariable @Positive Long organizationId) {
      organizationUserService.leaveCurrentOrganization(organizationId);
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/{organizationId}/join")
   public ResponseEntity<Void> joinPublicOrganization(
         @PathVariable @Positive Long organizationId) {
      organizationUserService.requestToJoinPublicOrganization(organizationId);
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/{organizationId}/members")
   public ResponseEntity<Void> addMember(
         @PathVariable @Positive Long organizationId,
         @Valid @RequestBody OrganizationMemberAddRequestDTO request) {
      organizationUserService.addUserToOrganization(organizationId, request.getUserId());
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/{organizationId}/members/{userId}")
   public ResponseEntity<Void> removeMember(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long userId) {
      organizationUserService.removeUserFromOrganization(organizationId, userId);
      return ResponseEntity.noContent().build();
   }

   @GetMapping("/{organizationId}/members/page")
   public ResponseEntity<Page<OrganizationMemberDTO>> searchMembersPage(
         @PathVariable @Positive Long organizationId,
         @RequestParam(required = false) String query,
         @RequestParam(required = false) List<@Positive Long> roleIds,
         @RequestParam(required = false) List<@Positive Long> instrumentIds,
         @PageableDefault(size = 20) Pageable pageable) {
      return ResponseEntity.ok(
            organizationUserService.searchMembers(
                  organizationId,
                  query,
                  roleIds,
                  instrumentIds,
                  pageable));
   }

   @GetMapping("/{organizationId}/join-requests/pending")
   public ResponseEntity<List<OrganizationJoinRequestDTO>> getPendingJoinRequests(
         @PathVariable @Positive Long organizationId) {
      List<OrganizationUser> pending = organizationUserService.getPendingJoinRequests(organizationId);
      List<OrganizationJoinRequestDTO> dtos = pending.stream()
            .map(OrganizationJoinRequestDTO::fromEntity)
            .toList();
      return ResponseEntity.ok(dtos);
   }

   @PostMapping("/{organizationId}/join-requests/{userId}/approve")
   public ResponseEntity<Void> approveJoinRequest(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long userId) {
      organizationUserService.approveJoinRequest(organizationId, userId);
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/{organizationId}/join-requests/{userId}/reject")
   public ResponseEntity<Void> rejectJoinRequest(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long userId) {
      organizationUserService.rejectJoinRequest(organizationId, userId);
      return ResponseEntity.noContent().build();
   }

   @GetMapping("/public")
   public ResponseEntity<List<OrganizationDTO>> searchPublicOrganizations(
         @RequestParam(required = false) String name) {
      return ResponseEntity.ok(organizationService.searchPublicOrganizationsByName(name));
   }

   @GetMapping("/public/page")
   public ResponseEntity<Page<OrganizationDTO>> searchPublicOrganizationsPage(
         @RequestParam(required = false) String name,
         @PageableDefault(size = 10) Pageable pageable) {
      return ResponseEntity.ok(organizationService.searchPublicOrganizationsByName(name, pageable));
   }
}
