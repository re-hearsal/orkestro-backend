package io.github.Romariok.orkestro.section.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.user.dto.TechnicalRoleCreateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SectionTechnicalRoleControllerTest {

    @Mock
    private TechnicalRoleService technicalRoleService;

    @InjectMocks
    private SectionTechnicalRoleController sectionTechnicalRoleController;

    @Test
    void getSectionRoles_returnsOk() {
        TechnicalRoleDTO role = new TechnicalRoleDTO(1L, null, null, 5L, "Leader", false, List.of());
        when(technicalRoleService.getSectionRoles(5L)).thenReturn(List.of(role));

        var result = sectionTechnicalRoleController.getSectionRoles(5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Leader", result.getBody().get(0).getName());
    }

    @Test
    void createSectionRole_returnsCreated() {
        TechnicalRoleDTO role = new TechnicalRoleDTO(1L, null, null, 5L, "Musician", false, List.of());
        when(technicalRoleService.createSectionRole(eq(5L), any(TechnicalRoleCreateRequestDTO.class)))
                .thenReturn(role);
        var request = new TechnicalRoleCreateRequestDTO("Musician", List.of());

        var result = sectionTechnicalRoleController.createSectionRole(5L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Musician", result.getBody().getName());
    }

    @Test
    void updateSectionRole_returnsOk() {
        TechnicalRoleDTO updated = new TechnicalRoleDTO(1L, null, null, 5L, "Senior Musician", false, List.of());
        when(technicalRoleService.updateSectionRole(eq(5L), eq(1L), any(TechnicalRoleCreateRequestDTO.class)))
                .thenReturn(updated);
        var request = new TechnicalRoleCreateRequestDTO("Senior Musician", List.of());

        var result = sectionTechnicalRoleController.updateSectionRole(5L, 1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Senior Musician", result.getBody().getName());
    }

    @Test
    void deleteSectionRole_returnsNoContent() {
        doNothing().when(technicalRoleService).deleteSectionRole(5L, 1L);

        var result = sectionTechnicalRoleController.deleteSectionRole(5L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
