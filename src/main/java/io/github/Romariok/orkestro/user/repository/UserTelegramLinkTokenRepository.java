package io.github.Romariok.orkestro.user.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.user.models.UserTelegramLinkToken;

@Repository
public interface UserTelegramLinkTokenRepository extends JpaRepository<UserTelegramLinkToken, Long> {

      Optional<UserTelegramLinkToken> findByToken(String token);

      @Modifying
      @Query("""
                  DELETE FROM UserTelegramLinkToken t
                  WHERE (t.expiresAt IS NOT NULL AND t.expiresAt < :threshold)
                     OR (t.usedAt IS NOT NULL AND t.usedAt < :threshold)
                  """)
      void deleteOldTokens(@Param("threshold") Instant threshold);

      @Modifying
      @Query("""
                  UPDATE UserTelegramLinkToken t
                  SET t.expiresAt = :now
                  WHERE t.userId = :userId
                    AND t.usedAt IS NULL
                    AND (t.expiresAt IS NULL OR t.expiresAt > :now)
                  """)
      void invalidateActiveTokensForUser(@Param("userId") Long userId, @Param("now") Instant now);
}
