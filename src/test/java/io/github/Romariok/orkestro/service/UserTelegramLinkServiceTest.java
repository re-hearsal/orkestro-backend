package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserTelegramLinkToken;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserTelegramLinkTokenRepository;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTelegramLinkServiceTest {

      @Mock
      private UserRepository userRepository;

      @Mock
      private UserTelegramLinkTokenRepository tokenRepository;

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

            verify(tokenRepository, never()).save(any());
      }

      @Test
      void createLinkTokenForUser_validUser_createsTokenAndInvalidatesPrevious() {
            Long userId = 1L;
            User user = User.builder()
                        .id(userId)
                        .username("user")
                        .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any(UserTelegramLinkToken.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

            String generatedToken = userTelegramLinkService.createLinkTokenForUser(userId);

            // Проверяем, что предыдущие активные токены были инвалидированы
            verify(tokenRepository).invalidateActiveTokensForUser(eq(userId), any(Instant.class));

            // Проверяем, что новый токен корректно сохранён
            ArgumentCaptor<UserTelegramLinkToken> captor = ArgumentCaptor.forClass(UserTelegramLinkToken.class);
            verify(tokenRepository).save(captor.capture());
            UserTelegramLinkToken saved = captor.getValue();

            assertEquals(userId, saved.getUserId());
            assertEquals(generatedToken, saved.getToken());
            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getExpiresAt());
            // expiresAt должен быть позже createdAt (TTL > 0)
            assertEquals(true, saved.getExpiresAt().isAfter(saved.getCreatedAt()));
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
            when(tokenRepository.save(any(UserTelegramLinkToken.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));

            String token = userTelegramLinkService.createLinkTokenForCurrentUser();

            assertNotNull(token);
            verify(userRepository).findById(currentUserId);
            verify(tokenRepository).invalidateActiveTokensForUser(eq(currentUserId), any(Instant.class));
            verify(tokenRepository).save(any(UserTelegramLinkToken.class));
      }
}
