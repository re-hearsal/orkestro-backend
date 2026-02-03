package io.github.Romariok.orkestro.user.controller;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.user.dto.MusicalRoleUpdateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TelegramLinkTokenResponseDTO;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

   private final SecurityUtils securityUtils;
   private final MusicalRoleService musicalRoleService;
   private final UserTelegramLinkService userTelegramLinkService;

   @GetMapping("/me/musical-roles")
   public ResponseEntity<List<MusicalRoleDTO>> getMyMusicalRoles() {
      Long currentUserId = securityUtils.getCurrentUserId();
      return ResponseEntity.ok(musicalRoleService.getUserMusicalRoles(currentUserId));
   }

   @PostMapping("/me/musical-roles")
   public ResponseEntity<Void> setMyInstruments(@Valid @RequestBody MusicalRoleUpdateRequestDTO request) {
      Long currentUserId = securityUtils.getCurrentUserId();
      musicalRoleService.setUserInstruments(currentUserId, request.getInstrumentIds());
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/me/musical-roles/{instrumentId}")
   public ResponseEntity<Void> addMyInstrument(@PathVariable @Positive Long instrumentId) {
      Long currentUserId = securityUtils.getCurrentUserId();
      musicalRoleService.addInstrumentToUser(currentUserId, instrumentId);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/me/musical-roles/{instrumentId}")
   public ResponseEntity<Void> removeMyInstrument(@PathVariable @Positive Long instrumentId) {
      Long currentUserId = securityUtils.getCurrentUserId();
      musicalRoleService.removeInstrumentFromUser(currentUserId, instrumentId);
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/me/telegram/link-token")
   public ResponseEntity<TelegramLinkTokenResponseDTO> createTelegramLinkToken() {
      String token = userTelegramLinkService.createLinkTokenForCurrentUser();
      return ResponseEntity.ok(new TelegramLinkTokenResponseDTO(token));
   }
}

