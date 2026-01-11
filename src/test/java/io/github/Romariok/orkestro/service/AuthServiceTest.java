package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.security.JWTUtil;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.service.AuthService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

      @Mock
      private UserService userService;

      @Mock
      private PasswordEncoder passwordEncoder;

      @Mock
      private JWTUtil jwtUtil;

      @Mock
      private AuthenticationManager authenticationManager;

      @InjectMocks
      private AuthService authService;

      @AfterEach
      void clearSecurityContext() {
            SecurityContextHolder.clearContext();
      }

      @Test
      void changePassword_success_updatesPassword() {
            String username = "user";
            String currentPassword = "oldPass";
            String newPassword = "newPass";

            // Аутентифицированный пользователь
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, "credentials",
                        List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = User.builder()
                        .username(username)
                        .password("encoded-old")
                        .build();

            when(userService.findByUsername(username)).thenReturn(user);
            when(passwordEncoder.matches(currentPassword, "encoded-old")).thenReturn(true);
            when(passwordEncoder.encode(newPassword)).thenReturn("encoded-new");

            authService.changePassword(currentPassword, newPassword);

            assertEquals("encoded-new", user.getPassword());
            verify(userService).saveUser(user);
      }

      @Test
      void changePassword_incorrectCurrent_throwsBusinessException() {
            String username = "user";
            String currentPassword = "wrongPass";
            String newPassword = "newPass";

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, "credentials",
                        List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = User.builder()
                        .username(username)
                        .password("encoded-old")
                        .build();

            when(userService.findByUsername(username)).thenReturn(user);
            when(passwordEncoder.matches(currentPassword, "encoded-old")).thenReturn(false);

            assertThrows(
                        BusinessException.class,
                        () -> authService.changePassword(currentPassword, newPassword));

            verify(userService, never()).saveUser(any(User.class));
      }

      @Test
      void resetPassword_success_updatesPassword() {
            String username = "user";
            String newPassword = "newPass";

            User user = User.builder()
                        .username(username)
                        .password("old-encoded")
                        .build();

            when(userService.findByUsername(username)).thenReturn(user);
            when(passwordEncoder.encode(newPassword)).thenReturn("encoded-new");

            authService.resetPassword(username, newPassword);

            assertEquals("encoded-new", user.getPassword());
            verify(userService).saveUser(user);
      }

      @Test
      void resetPassword_userNotFound_throwsEntityNotFound() {
            String username = "unknown";
            String newPassword = "newPass";

            when(userService.findByUsername(username))
                        .thenThrow(new UsernameNotFoundException("User not found"));

            assertThrows(
                        EntityNotFoundException.class,
                        () -> authService.resetPassword(username, newPassword));
      }

      @Test
      void logout_clearsSecurityContext() {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user", "credentials",
                        List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            authService.logout();

            // После logout аутентификация должна быть очищена
            assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
      }
}
