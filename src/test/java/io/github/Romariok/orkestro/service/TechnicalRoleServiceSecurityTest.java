package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class TechnicalRoleServiceSecurityTest {

   @Test
   void assignOrganizationRoleToUser_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = TechnicalRoleService.class
            .getMethod("assignOrganizationRoleToUser", Long.class, Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_ASSIGN_TECH_ROLE')",
            expr);
   }

   @Test
   void removeOrganizationRoleFromUser_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = TechnicalRoleService.class
            .getMethod("removeOrganizationRoleFromUser", Long.class, Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_ASSIGN_TECH_ROLE')",
            expr);
   }

   @Test
   void assignSectionRoleToUser_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = TechnicalRoleService.class
            .getMethod("assignSectionRoleToUser", Long.class, Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_ASSIGN_TECH_ROLE')",
            expr);
   }

   @Test
   void removeSectionRoleFromUser_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = TechnicalRoleService.class
            .getMethod("removeSectionRoleFromUser", Long.class, Long.class, Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_ASSIGN_TECH_ROLE')",
            expr);
   }
}
