package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import io.github.Romariok.orkestro.repository.UserTelegramLinkTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
