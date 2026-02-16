package io.github.Romariok.orkestro.user.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTelegramLinkTokenService {

   @Value("${orkestro.telegram.link.secret:${jwt.secret:secret}}")
   private String secret;

   @Value("${orkestro.telegram.link.ttl-minutes:60}")
   private long ttlMinutes;

   public String createToken(Long userId) {
      Instant now = Instant.now();
      Instant expiresAt = now.plusSeconds(ttlMinutes * 60);

      return Jwts.builder()
            .setSubject("telegram-link")
            .claim("userId", userId)
            .claim("ttlMinutes", ttlMinutes)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .signWith(signingKey(), SignatureAlgorithm.HS256)
            .compact();
   }

   public ParsedTelegramLinkToken parseToken(String token) {
      Claims claims = Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .getBody();

      String subject = claims.getSubject();
      if (!"telegram-link".equals(subject)) {
         throw new IllegalArgumentException("Invalid Telegram link token subject");
      }

      Number userIdRaw = claims.get("userId", Number.class);
      if (userIdRaw == null) {
         throw new IllegalArgumentException("Invalid Telegram link token payload: missing userId");
      }

      Number ttlMinutesRaw = claims.get("ttlMinutes", Number.class);
      Long ttlFromToken = ttlMinutesRaw == null ? null : ttlMinutesRaw.longValue();

      return new ParsedTelegramLinkToken(userIdRaw.longValue(), ttlFromToken);
   }

   private Key signingKey() {
      byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
      if (bytes.length < 32) {
         bytes = Arrays.copyOf(bytes, 32);
      }
      return Keys.hmacShaKeyFor(bytes);
   }

   public record ParsedTelegramLinkToken(
         Long userId,
         Long ttlMinutes) {
   }
}
