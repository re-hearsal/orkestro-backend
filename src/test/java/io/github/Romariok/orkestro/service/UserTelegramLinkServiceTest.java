package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkTokenService;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTelegramLinkServiceTest {

      @Mock
      private UserRepository userRepository;

      @Mock
      private UserTelegramLinkTokenService tokenService;

      @Mock
      private SecurityUtils securityUtils;

      @InjectMocks
      private UserTelegramLinkService userTelegramLinkService;

      @Test
      void createLinkTokenForUser_userNotFound_throwsEntityNotFound() {
            Long userId = 1L;
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                        () -> userTelegramLinkService.createLinkTokenForUser(userId));

            verify(tokenService, never()).createToken(any());
      }

      @Test
      void createLinkTokenForUser_validUser_createsToken() {
            Long userId = 1L;
            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenService.createToken(userId)).thenReturn("generated-token");

            String generatedToken = userTelegramLinkService.createLinkTokenForUser(userId);

            assertEquals("generated-token", generatedToken);
            verify(tokenService).createToken(userId);
      }

      @Test
      void createLinkTokenForCurrentUser_usesCurrentUserIdFromSecurityUtils() {
            Long currentUserId = 5L;
            User user = User.builder()
                        .id(currentUserId)
                        .username("current")
                        .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(user));
            when(tokenService.createToken(currentUserId)).thenReturn("token");

            String token = userTelegramLinkService.createLinkTokenForCurrentUser();

            assertNotNull(token);
            verify(userRepository).findById(currentUserId);
            verify(tokenService).createToken(currentUserId);
      }
}
