package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserTelegramLinkService {

   private final UserRepository userRepository;
   private final UserTelegramLinkTokenService tokenService;
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

      String token = tokenService.createToken(user.getId());

      log.info("Created Telegram link token for user id={}", userId);
      return token;
   }
}
