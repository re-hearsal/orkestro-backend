package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;


class OrganizationUserServiceSecurityTest {

   @Test
   void getPendingJoinRequests_hasPreAuthorizeWithViewPermissions() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("getPendingJoinRequests", Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertTrue(expr.contains("ORG_JOIN_REQUEST_VIEW"));
      assertTrue(expr.contains("CTX_PERM_ORG"));
   }

   @Test
   void approveJoinRequest_hasPreAuthorizeWithManagePermissions() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("approveJoinRequest", Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertTrue(expr.contains("ORG_JOIN_REQUEST_MANAGE"));
      assertTrue(expr.contains("CTX_PERM_ORG"));
   }

   @Test
   void rejectJoinRequest_hasPreAuthorizeWithManagePermissions() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("rejectJoinRequest", Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertTrue(expr.contains("ORG_JOIN_REQUEST_MANAGE"));
      assertTrue(expr.contains("CTX_PERM_ORG"));
   }
}
