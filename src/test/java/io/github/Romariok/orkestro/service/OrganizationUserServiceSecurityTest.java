package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class OrganizationUserServiceSecurityTest {

   @Test
   void getPendingJoinRequests_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("getPendingJoinRequests", Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_VIEW')",
            expr);
   }

   @Test
   void approveJoinRequest_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("approveJoinRequest", Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')",
            expr);
   }

   @Test
   void rejectJoinRequest_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationUserService.class
            .getMethod("rejectJoinRequest", Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_JOIN_REQUEST_MANAGE')",
            expr);
   }
}
