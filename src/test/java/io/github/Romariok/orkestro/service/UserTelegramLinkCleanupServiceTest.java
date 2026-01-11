package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.Romariok.orkestro.user.repository.UserTelegramLinkTokenRepository;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkCleanupService;

@ExtendWith(MockitoExtension.class)
class UserTelegramLinkCleanupServiceTest {

   @Mock
   private UserTelegramLinkTokenRepository tokenRepository;

   @InjectMocks
   private UserTelegramLinkCleanupService cleanupService;

   @Test
   void cleanOldTokens_delegatesToRepository() {
      cleanupService.cleanOldTokens();

      verify(tokenRepository).deleteOldTokens(any(Instant.class));
   }
}
