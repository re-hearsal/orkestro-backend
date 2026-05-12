package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.security.JWTUtil;
import io.github.Romariok.orkestro.user.dto.AuthResponseDTO;
import io.github.Romariok.orkestro.user.dto.LoginRequestDTO;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.service.AuthService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

      @Mock
      private FileStorageService fileStorageService;

      @Mock
      private FileRollbackHelper fileRollbackHelper;

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

      @Test
      void register_usernameAlreadyTaken_throwsBusinessException() {
            io.github.Romariok.orkestro.user.dto.RegisterRequestDTO request =
                        new io.github.Romariok.orkestro.user.dto.RegisterRequestDTO();
            request.setUsername("existingUser");
            request.setName("Existing User");
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setPreferredLanguage(io.github.Romariok.orkestro.user.models.enums.UserLanguageType.RU);
            request.setBirthDate(java.time.LocalDate.of(1990, 1, 1));

            when(userService.existsByUsername("existingUser")).thenReturn(true);

            assertThrows(BusinessException.class, () -> authService.register(request));

            verify(userService, never()).saveUser(any(User.class));
      }

      @Test
      void login_success_returnsAuthResponseWithToken() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setUsername("user");
            request.setPassword("password");

            Authentication auth = new UsernamePasswordAuthenticationToken("user", "password", List.of());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

            UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);
            when(userDetails.getAuthorities()).thenReturn(List.of());
            when(userService.loadUserByUsername("user")).thenReturn(userDetails);
            when(jwtUtil.generateToken(eq("user"), anySet())).thenReturn("jwt-token");

            AuthResponseDTO result = authService.login(request);

            assertNotNull(result);
            assertEquals("jwt-token", result.getToken());
            assertEquals("user", result.getUsername());
      }

      @Test
      void login_badCredentials_throwsBadCredentialsException() {
            LoginRequestDTO request = new LoginRequestDTO();
            request.setUsername("user");
            request.setPassword("wrong");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                        .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(BadCredentialsException.class, () -> authService.login(request));
      }

      @Test
      void register_success_noAvatar_returnsAuthResponse() {
            io.github.Romariok.orkestro.user.dto.RegisterRequestDTO request =
                        new io.github.Romariok.orkestro.user.dto.RegisterRequestDTO();
            request.setUsername("newUser");
            request.setName("New User");
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setPreferredLanguage(io.github.Romariok.orkestro.user.models.enums.UserLanguageType.EN);
            request.setBirthDate(java.time.LocalDate.of(1995, 6, 15));

            when(userService.existsByUsername("newUser")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");

            UserDetails userDetails = org.mockito.Mockito.mock(UserDetails.class);
            when(userDetails.getAuthorities()).thenReturn(List.of());
            when(userService.loadUserByUsername("newUser")).thenReturn(userDetails);
            when(jwtUtil.generateToken(eq("newUser"), anySet())).thenReturn("new-jwt-token");

            AuthResponseDTO result = authService.register(request);

            assertNotNull(result);
            assertEquals("new-jwt-token", result.getToken());
            assertEquals("newUser", result.getUsername());
            verify(userService).saveUser(any(User.class));
      }
}
