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

   private void setField(String fieldName, Object value) throws Exception {
      Field field = UserTelegramLinkTokenService.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(tokenService, value);
   }
}
