package io.github.Romariok.orkestro.organization.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;

import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberTechnicalRoleControllerTest {

    @Mock
    private TechnicalRoleService technicalRoleService;

    @InjectMocks
    private OrganizationMemberTechnicalRoleController organizationMemberTechnicalRoleController;

    @Test
    void assignOrganizationRoleToUser_returnsNoContent() {
        doNothing().when(technicalRoleService).assignOrganizationRoleToUser(10L, 42L, 3L);

        var result = organizationMemberTechnicalRoleController.assignOrganizationRoleToUser(10L, 42L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void removeOrganizationRoleFromUser_returnsNoContent() {
        doNothing().when(technicalRoleService).removeOrganizationRoleFromUser(10L, 42L, 3L);

        var result = organizationMemberTechnicalRoleController.removeOrganizationRoleFromUser(10L, 42L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
