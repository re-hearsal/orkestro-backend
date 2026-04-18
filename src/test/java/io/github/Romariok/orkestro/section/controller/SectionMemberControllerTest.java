package io.github.Romariok.orkestro.section.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.section.dto.SectionMemberDTO;
import io.github.Romariok.orkestro.section.service.SectionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SectionMemberControllerTest {

    @Mock
    private SectionService sectionService;

    @InjectMocks
    private SectionMemberController sectionMemberController;

    @Test
    void addUserToSection_returnsNoContent() {
        doNothing().when(sectionService).addUserToSection(1L, 2L);

        var result = sectionMemberController.addUserToSection(1L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void removeUserFromSection_returnsNoContent() {
        doNothing().when(sectionService).removeUserFromSection(1L, 2L);

        var result = sectionMemberController.removeUserFromSection(1L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void leaveSection_returnsNoContent() {
        doNothing().when(sectionService).leaveCurrentSection(1L);

        var result = sectionMemberController.leaveSection(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void searchMembersPage_returnsOk() {
        SectionMemberDTO member = new SectionMemberDTO(2L, "john_doe", "John Doe", null, Instant.parse("2026-01-01T00:00:00Z"));
        Page<SectionMemberDTO> page = new PageImpl<>(List.of(member));
        when(sectionService.searchMembers(eq(1L), isNull(), isNull(), isNull(), any()))
                .thenReturn(page);

        var result = sectionMemberController.searchMembersPage(1L, null, null, null, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals("john_doe", result.getBody().getContent().get(0).getUsername());
    }
}
