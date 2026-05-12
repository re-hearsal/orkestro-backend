package io.github.Romariok.orkestro.section.controller;

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
class SectionMemberTechnicalRoleControllerTest {

    @Mock
    private TechnicalRoleService technicalRoleService;

    @InjectMocks
    private SectionMemberTechnicalRoleController controller;

    @Test
    void assignSectionRoleToUser_returnsNoContent() {
        doNothing().when(technicalRoleService).assignSectionRoleToUser(1L, 2L, 3L);

        var result = controller.assignSectionRoleToUser(1L, 2L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void removeSectionRoleFromUser_returnsNoContent() {
        doNothing().when(technicalRoleService).removeSectionRoleFromUser(1L, 2L, 3L);

        var result = controller.removeSectionRoleFromUser(1L, 2L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
