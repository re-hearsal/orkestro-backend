package io.github.Romariok.orkestro.section.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrganizationSectionControllerTest {

    @Mock
    private SectionService sectionService;

    @InjectMocks
    private OrganizationSectionController organizationSectionController;

    @Test
    void createSectionInOrganization_returnsCreated() {
        SectionDTO response = new SectionDTO(1L, "Strings", "String instruments section", 10L, null);
        when(sectionService.createSectionInOrganization(eq(10L), any(SectionCreateRequestDTO.class)))
                .thenReturn(response);

        SectionCreateRequestDTO request = new SectionCreateRequestDTO("Strings", "String instruments section");

        var result = organizationSectionController.createSectionInOrganization(10L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Strings", result.getBody().getName());
        assertEquals(10L, result.getBody().getOrganizationId());
    }
}
