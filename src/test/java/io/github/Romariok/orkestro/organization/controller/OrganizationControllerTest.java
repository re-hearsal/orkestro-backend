package io.github.Romariok.orkestro.organization.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.dto.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.service.OrganizationService;
import io.github.Romariok.orkestro.organization.service.OrganizationUserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationUserService organizationUserService;

    @InjectMocks
    private OrganizationController organizationController;

    @Test
    void createOrganization_returnsCreated() {
        OrganizationDTO response = new OrganizationDTO(
                1L, "Rock Band", "Moscow", "Professional rock band", null,
                Instant.parse("2026-02-18T10:00:00Z"), List.of());
        when(organizationService.createOrganization(any(OrganizationCreateRequestDTO.class))).thenReturn(response);

        var result = organizationController.createOrganization(new OrganizationCreateRequestDTO());

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Rock Band", result.getBody().getName());
    }

    @Test
    void getOrganization_returnsOk() {
        OrganizationDTO response = new OrganizationDTO(
                5L, "Symphony Orchestra", "Saint Petersburg", "Classical orchestra", null,
                Instant.parse("2026-01-01T00:00:00Z"), List.of());
        when(organizationService.getOrganization(eq(5L))).thenReturn(response);

        var result = organizationController.getOrganization(5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(5L, result.getBody().getId());
        assertEquals("Symphony Orchestra", result.getBody().getName());
    }

    @Test
    void updateOrganization_returnsOk() {
        OrganizationDTO response = new OrganizationDTO(
                5L, "Updated Orchestra", "Moscow", "Updated description", null,
                Instant.parse("2026-01-01T00:00:00Z"), List.of());
        when(organizationService.updateOrganization(eq(5L), any(OrganizationUpdateRequestDTO.class)))
                .thenReturn(response);

        OrganizationUpdateRequestDTO request = new OrganizationUpdateRequestDTO();
        request.setName("Updated Orchestra");

        var result = organizationController.updateOrganization(5L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Updated Orchestra", result.getBody().getName());
    }

    @Test
    void deleteOrganization_returnsNoContent() {
        doNothing().when(organizationService).deleteOrganization(5L);

        var result = organizationController.deleteOrganization(5L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void leaveOrganization_returnsNoContent() {
        doNothing().when(organizationUserService).leaveCurrentOrganization(5L);

        var result = organizationController.leaveOrganization(5L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void searchPublicOrganizations_returnsOk() {
        List<OrganizationDTO> organizations = List.of(
                new OrganizationDTO(1L, "Rock Band", "Moscow", null, null, null, List.of()),
                new OrganizationDTO(2L, "Rock Ensemble", "SPB", null, null, null, List.of())
        );
        when(organizationService.searchPublicOrganizationsByName(eq("Rock"))).thenReturn(organizations);

        var result = organizationController.searchPublicOrganizations("Rock");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }
}
