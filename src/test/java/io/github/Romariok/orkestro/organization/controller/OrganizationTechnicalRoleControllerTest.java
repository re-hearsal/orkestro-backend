package io.github.Romariok.orkestro.organization.controller;

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
class OrganizationTechnicalRoleControllerTest {

    @Mock
    private TechnicalRoleService technicalRoleService;

    @InjectMocks
    private OrganizationTechnicalRoleController organizationTechnicalRoleController;

    @Test
    void getOrganizationRoles_returnsOk() {
        List<TechnicalRoleDTO> roles = List.of(
                new TechnicalRoleDTO(1L, null, 10L, null, "Conductor", true, List.of("EVENT_MANAGE")),
                new TechnicalRoleDTO(2L, null, 10L, null, "Soloist", false, List.of())
        );
        when(technicalRoleService.getOrganizationRoles(eq(10L))).thenReturn(roles);

        var result = organizationTechnicalRoleController.getOrganizationRoles(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals("Conductor", result.getBody().get(0).getName());
    }

    @Test
    void createOrganizationRole_returnsCreated() {
        TechnicalRoleDTO response = new TechnicalRoleDTO(
                3L, null, 10L, null, "Arranger", false, List.of("SHEET_MUSIC_MANAGE"));
        when(technicalRoleService.createOrganizationRole(eq(10L), any(TechnicalRoleCreateRequestDTO.class)))
                .thenReturn(response);

        TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                "Arranger", List.of("SHEET_MUSIC_MANAGE"));

        var result = organizationTechnicalRoleController.createOrganizationRole(10L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(3L, result.getBody().getId());
        assertEquals("Arranger", result.getBody().getName());
    }

    @Test
    void updateOrganizationRole_returnsOk() {
        TechnicalRoleDTO response = new TechnicalRoleDTO(
                2L, null, 10L, null, "Senior Soloist", false, List.of("EVENT_READ"));
        when(technicalRoleService.updateOrganizationRole(eq(10L), eq(2L), any(TechnicalRoleCreateRequestDTO.class)))
                .thenReturn(response);

        TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                "Senior Soloist", List.of("EVENT_READ"));

        var result = organizationTechnicalRoleController.updateOrganizationRole(10L, 2L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Senior Soloist", result.getBody().getName());
    }

    @Test
    void deleteOrganizationRole_returnsNoContent() {
        doNothing().when(technicalRoleService).deleteOrganizationRole(10L, 2L);

        var result = organizationTechnicalRoleController.deleteOrganizationRole(10L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
