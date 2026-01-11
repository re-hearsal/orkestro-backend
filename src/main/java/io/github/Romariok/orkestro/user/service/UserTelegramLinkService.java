package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserTelegramLinkToken;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserTelegramLinkTokenRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTelegramLinkService {

   private final UserRepository userRepository;
   private final UserTelegramLinkTokenRepository tokenRepository;
   private final SecurityUtils securityUtils;

   @Transactional
   public String createLinkTokenForCurrentUser() {
      Long currentUserId = securityUtils.getCurrentUserId();
      return createLinkTokenForUser(currentUserId);
   }

   @Transactional
   public String createLinkTokenForUser(Long userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      String token = UUID.randomUUID().toString();
      Instant now = Instant.now();
      Instant expiresAt = now.plus(1, ChronoUnit.HOURS);


      tokenRepository.invalidateActiveTokensForUser(user.getId(), now);

      UserTelegramLinkToken linkToken = UserTelegramLinkToken.builder()
            .userId(user.getId())
            .token(token)
            .createdAt(now)
            .expiresAt(expiresAt)
            .build();

      tokenRepository.save(linkToken);

      log.info("Created Telegram link token for user id={}", userId);
      return token;
   }
}
