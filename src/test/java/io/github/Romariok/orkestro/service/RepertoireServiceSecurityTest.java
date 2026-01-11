package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.repertoire.service.RepertoireService;

class RepertoireServiceSecurityTest {

   @Test
   void createSong_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = RepertoireService.class
            .getMethod("createSong", Long.class, SongCreateRequestDTO.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':REPERTOIRE_CREATE_SONG')",
            expr);
   }

   @Test
   void updateSong_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = RepertoireService.class
            .getMethod("updateSong", Long.class, SongUpdateRequestDTO.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + @songRepository.findById(#songId).orElse(null)?.organizationId + ':REPERTOIRE_EDIT_SONG')",
            expr);
   }

   @Test
   void deleteSong_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = RepertoireService.class
            .getMethod("deleteSong", Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + @songRepository.findById(#songId).orElse(null)?.organizationId + ':REPERTOIRE_DELETE_SONG')",
            expr);
   }
}
