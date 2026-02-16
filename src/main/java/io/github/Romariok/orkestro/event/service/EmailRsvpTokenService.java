package io.github.Romariok.orkestro.event.service;

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
public class EmailRsvpTokenService {

    @Value("${orkestro.email.rsvp.secret:${jwt.secret:secret}}")
    private String secret;

    @Value("${orkestro.email.rsvp.ttl-minutes:10080}")
    private long ttlMinutes;

    public String createToken(Long eventId, Long userId, boolean accepted) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlMinutes * 60);

        return Jwts.builder()
                .setSubject("email-rsvp")
                .claim("eventId", eventId)
                .claim("userId", userId)
                .claim("accepted", accepted)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public ParsedEmailRsvpToken parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        Long eventId = claims.get("eventId", Number.class).longValue();
        Long userId = claims.get("userId", Number.class).longValue();
        Boolean accepted = claims.get("accepted", Boolean.class);
        if (accepted == null) {
            throw new IllegalArgumentException("Invalid RSVP token payload: missing accepted flag");
        }
        return new ParsedEmailRsvpToken(eventId, userId, accepted);
    }

    private Key signingKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = Arrays.copyOf(bytes, 32);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public record ParsedEmailRsvpToken(
            Long eventId,
            Long userId,
            boolean accepted) {
    }
}
