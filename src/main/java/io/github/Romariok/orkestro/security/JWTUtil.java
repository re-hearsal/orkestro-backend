package io.github.Romariok.orkestro.security;


import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JWTUtil {
    @Value("${jwt.secret}")
    private String secretKeyString;

    @Value("${jwt.expiration}")
    private long expirationTime;
    
    private Key secretKey;
    
    @PostConstruct
    public void init() {
        if (secretKeyString != null && !secretKeyString.isBlank()) {
            try {
                secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKeyString));
                log.info("Using provided JWT secret key");
                return;
            } catch (IllegalArgumentException | WeakKeyException e) {
                log.warn("Provided JWT secret key is invalid or too weak. Generating a secure key...", e);
            }
        } else {
            log.warn("JWT secret key is not configured. Generating a secure key...");
        }

        secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        log.info("Using generated secure JWT secret key");
    }

    public String generateToken(String username, Set<String> authorities) {
        String authoritiesString = String.join(",", authorities);
        return Jwts.builder()
                .setSubject(username)
                .claim("authorities", authoritiesString)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody().getSubject();
    }

    public Set<String> getAuthoritiesFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        String authorities = claims.get("authorities", String.class);
        return Set.of(authorities.split(","));
    }

    public String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}