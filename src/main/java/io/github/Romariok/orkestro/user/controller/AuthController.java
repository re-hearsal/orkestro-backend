package io.github.Romariok.orkestro.user.controller;

import io.github.Romariok.orkestro.user.dto.AuthResponseDTO;
import io.github.Romariok.orkestro.user.dto.LoginRequestDTO;
import io.github.Romariok.orkestro.user.dto.PasswordResetRequestDTO;
import io.github.Romariok.orkestro.user.dto.RegisterRequestDTO;
import io.github.Romariok.orkestro.user.dto.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.user.service.AuthService;
import io.github.Romariok.orkestro.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

   private final AuthService authService;
   private final UserService userService;

   @PostMapping("/register")
   public ResponseEntity<AuthResponseDTO> register(
         @Valid @ModelAttribute RegisterRequestDTO request) {
      AuthResponseDTO response = authService.register(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
   }

   @PostMapping("/login")
   public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
      return ResponseEntity.ok(authService.login(request));
   }

   @PostMapping("/logout")
   public ResponseEntity<Void> logout() {
      authService.logout();
      return ResponseEntity.noContent().build();
   }

   @PostMapping("/password/reset")
   public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
      authService.resetPassword(request.getUsername(), request.getNewPassword());
      return ResponseEntity.noContent().build();
   }

   @PatchMapping("/profile")
   public ResponseEntity<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequestDTO request) {
      userService.updateCurrentUserProfile(request);
      return ResponseEntity.noContent().build();
   }

   @DeleteMapping("/account")
   public ResponseEntity<Void> deleteAccount() {
      userService.deleteCurrentUserAccount();
      authService.logout();
      return ResponseEntity.noContent().build();
   }
}
