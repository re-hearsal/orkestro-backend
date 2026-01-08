package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.Romariok.orkestro.dto.section.SectionCreateRequestDTO;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SectionServiceSecurityTest {

    @Test
    void createSectionInOrganization_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = SectionService.class
                .getMethod("createSectionInOrganization", Long.class, SectionCreateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "hasAuthority('CTX_PERM_ORG:' + #organizationId + ':SECTION_CREATE')",
                expr);
    }

    @Test
    void createSectionInSection_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = SectionService.class
                .getMethod("createSectionInSection", Long.class, SectionCreateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "hasAuthority('CTX_PERM_SECTION:' + #parentSectionId + ':SECTION_CREATE')",
                expr);
    }

    @Test
    void deleteSection_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = SectionService.class
                .getMethod("deleteSection", Long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_DELETE')",
                expr);
    }
}

