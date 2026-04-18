package io.github.Romariok.orkestro.section.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
class SectionControllerTest {

    @Mock
    private SectionService sectionService;

    @InjectMocks
    private SectionController sectionController;

    @Test
    void createSectionInSection_returnsCreated() {
        SectionDTO response = new SectionDTO(2L, "Jazz Ensemble", "Jazz music group", 10L, 1L);
        when(sectionService.createSectionInSection(eq(1L), any(SectionCreateRequestDTO.class)))
                .thenReturn(response);

        SectionCreateRequestDTO request = new SectionCreateRequestDTO("Jazz Ensemble", "Jazz music group");

        var result = sectionController.createSectionInSection(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(2L, result.getBody().getId());
        assertEquals("Jazz Ensemble", result.getBody().getName());
        assertEquals(1L, result.getBody().getParentSectionId());
    }

    @Test
    void deleteSection_returnsNoContent() {
        doNothing().when(sectionService).deleteSection(2L);

        var result = sectionController.deleteSection(2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void leaveSection_returnsNoContent() {
        doNothing().when(sectionService).leaveCurrentSection(2L);

        var result = sectionController.leaveSection(2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
