package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.Romariok.orkestro.user.service.UserTelegramLinkTokenService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserTelegramLinkTokenServiceTest {

   private UserTelegramLinkTokenService tokenService;

   @BeforeEach
   void setUp() throws Exception {
      tokenService = new UserTelegramLinkTokenService();
      setField("secret", "telegram-link-secret-key-very-strong-123");
      setField("ttlMinutes", 60L);
   }

   @Test
   void createToken_generatesCompactTokenForTelegramStartParam() {
      String token = tokenService.createToken(1L);
      assertTrue(token.length() <= 64, "Telegram start payload must be compact");
      assertTrue(!token.contains("."), "Telegram start payload must not contain dot");
   }

   @Test
   void parseToken_invalidToken_throwsIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> tokenService.parseToken("bad_token_value"));
   }

   @Test
   void createAndParseToken_roundtrip_returnsCorrectUserId() {
      Long userId = 42L;
      String token = tokenService.createToken(userId);

      UserTelegramLinkTokenService.ParsedTelegramLinkToken parsed = tokenService.parseToken(token);

      assertTrue(parsed.userId().equals(userId));
      assertTrue(parsed.ttlMinutes() == 60L);
   }

   @Test
   void parseToken_tamperedToken_throwsIllegalArgumentException() throws Exception {
      Long userId = 42L;
      String token = tokenService.createToken(userId);

      // Flip last character to tamper with signature
      String tampered = token.substring(0, token.length() - 2) + "XX";

      assertThrows(IllegalArgumentException.class, () -> tokenService.parseToken(tampered));
   }

   @Test
   void parseToken_expiredToken_throwsIllegalArgumentException() throws Exception {
      // Create a token service with very short TTL (negative to ensure expiry)
      UserTelegramLinkTokenService expiredService = new UserTelegramLinkTokenService();
      setFieldOn(expiredService, "secret", "telegram-link-secret-key-very-strong-123");
      setFieldOn(expiredService, "ttlMinutes", -1L); // already expired

      String token = expiredService.createToken(10L);

      assertThrows(IllegalArgumentException.class, () -> expiredService.parseToken(token));
   }

   @Test
   void createToken_isCompactEnoughForTelegramStart() {
      for (long id : new long[]{1L, 9999999L, Long.MAX_VALUE}) {
         String token = tokenService.createToken(id);
         assertTrue(token.length() <= 64, "Token too long for user " + id + ": " + token.length());
      }
   }

   private void setField(String fieldName, Object value) throws Exception {
      Field field = UserTelegramLinkTokenService.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(tokenService, value);
   }

   private void setFieldOn(UserTelegramLinkTokenService service, String fieldName, Object value) throws Exception {
      Field field = UserTelegramLinkTokenService.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(service, value);
   }
}
