package io.github.Romariok.orkestro.user.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTelegramLinkTokenService {

   private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
   private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
   private static final String HMAC_ALGORITHM = "HmacSHA256";
   private static final int SIGNATURE_SIZE_BYTES = 16;

   @Value("${orkestro.telegram.link.secret:${jwt.secret:secret}}")
   private String secret;

   @Value("${orkestro.telegram.link.ttl-minutes:60}")
   private long ttlMinutes;

   public String createToken(Long userId) {
      Instant now = Instant.now();
      long expiresAtEpochSec = now.plusSeconds(ttlMinutes * 60).getEpochSecond();

      byte[] payload = ByteBuffer.allocate(Long.BYTES * 2)
            .putLong(userId)
            .putLong(expiresAtEpochSec)
            .array();
      byte[] signature = Arrays.copyOf(sign(payload), SIGNATURE_SIZE_BYTES);
      byte[] tokenBytes = ByteBuffer.allocate(payload.length + signature.length)
            .put(payload)
            .put(signature)
            .array();

      return URL_ENCODER.encodeToString(tokenBytes);
   }

   public ParsedTelegramLinkToken parseToken(String token) {
      byte[] tokenBytes;
      try {
         tokenBytes = URL_DECODER.decode(token);
      } catch (IllegalArgumentException ex) {
         throw new IllegalArgumentException("Invalid Telegram link token encoding");
      }

      if (tokenBytes.length != Long.BYTES * 2 + SIGNATURE_SIZE_BYTES) {
         throw new IllegalArgumentException("Invalid Telegram link token format");
      }

      byte[] payload = Arrays.copyOfRange(tokenBytes, 0, Long.BYTES * 2);
      byte[] signature = Arrays.copyOfRange(tokenBytes, Long.BYTES * 2, tokenBytes.length);

      if (payload.length != Long.BYTES * 2) {
         throw new IllegalArgumentException("Invalid Telegram link token payload");
      }
      if (signature.length != SIGNATURE_SIZE_BYTES) {
         throw new IllegalArgumentException("Invalid Telegram link token signature");
      }

      byte[] expectedSignature = Arrays.copyOf(sign(payload), SIGNATURE_SIZE_BYTES);
      if (!MessageDigest.isEqual(signature, expectedSignature)) {
         throw new IllegalArgumentException("Invalid Telegram link token signature");
      }

      ByteBuffer payloadBuffer = ByteBuffer.wrap(payload);
      long userId = payloadBuffer.getLong();
      long expiresAtEpochSec = payloadBuffer.getLong();

      if (expiresAtEpochSec <= Instant.now().getEpochSecond()) {
         throw new IllegalArgumentException("Telegram link token expired");
      }

      return new ParsedTelegramLinkToken(userId, ttlMinutes);
   }

   private byte[] sign(byte[] payload) {
      try {
         Mac mac = Mac.getInstance(HMAC_ALGORITHM);
         mac.init(new SecretKeySpec(signingKeyBytes(), HMAC_ALGORITHM));
         return mac.doFinal(payload);
      } catch (Exception ex) {
         throw new IllegalStateException("Failed to sign Telegram link token", ex);
      }
   }

   private byte[] signingKeyBytes() {
      byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
      return bytes.length < 32 ? Arrays.copyOf(bytes, 32) : bytes;
   }

   public record ParsedTelegramLinkToken(
         Long userId,
         Long ttlMinutes) {
   }
}
