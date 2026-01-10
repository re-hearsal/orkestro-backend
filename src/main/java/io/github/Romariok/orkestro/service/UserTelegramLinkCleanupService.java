package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.repository.UserTelegramLinkTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTelegramLinkCleanupService {

   private final UserTelegramLinkTokenRepository tokenRepository;

   @Scheduled(cron = "0 0 0 7 * *")
   @Transactional
   public void cleanOldTokens() {
      Instant threshold = Instant.now().minus(1, ChronoUnit.HOURS);
      tokenRepository.deleteOldTokens(threshold);
      log.debug("Cleaned up old Telegram link tokens older than {}", threshold);
   }
}
