package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.Romariok.orkestro.organization.dto.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.organization.service.OrganizationService;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class OrganizationServiceSecurityTest {

   @Test
   void updateOrganization_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationService.class
            .getMethod("updateOrganization", Long.class, OrganizationUpdateRequestDTO.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_EDIT')",
            expr);
   }

   @Test
   void deleteOrganization_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationService.class
            .getMethod("deleteOrganization", Long.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_DELETE')",
            expr);
   }

   @Test
   void setVisibility_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
      Method method = OrganizationService.class
            .getMethod("setVisibility", Long.class, VisibilityLevelType.class);

      PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

      String expr = preAuthorize.value();
      assertEquals(
            "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_SET_VISIBILITY')",
            expr);
   }
}
