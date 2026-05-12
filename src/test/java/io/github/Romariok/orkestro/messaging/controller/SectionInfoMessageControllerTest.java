package io.github.Romariok.orkestro.messaging.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageCreateRequestDTO;
import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageDTO;
import io.github.Romariok.orkestro.messaging.service.OrgInfoMessageService;
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
class SectionInfoMessageControllerTest {

    @Mock
    private OrgInfoMessageService orgInfoMessageService;

    @InjectMocks
    private SectionInfoMessageController sectionInfoMessageController;

    private OrgInfoMessageDTO buildMessage(Long id) {
        return OrgInfoMessageDTO.builder()
                .id(id)
                .sectionId(5L)
                .authorUserId(10L)
                .authorName("Section Leader")
                .text("Section practice tomorrow at 7pm")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void postMessage_returnsCreated() {
        // given
        OrgInfoMessageDTO created = buildMessage(1L);
        when(orgInfoMessageService.postSectionMessage(eq(5L), eq("Section practice tomorrow at 7pm")))
                .thenReturn(created);
        OrgInfoMessageCreateRequestDTO request = new OrgInfoMessageCreateRequestDTO();
        request.setText("Section practice tomorrow at 7pm");

        // when
        var result = sectionInfoMessageController.postMessage(5L, request);

        // then
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Section practice tomorrow at 7pm", result.getBody().getText());
    }

    @Test
    void getMessages_returnsOk() {
        // given
        Page<OrgInfoMessageDTO> page = new PageImpl<>(List.of(buildMessage(1L), buildMessage(2L)));
        when(orgInfoMessageService.getSectionMessages(eq(5L), any())).thenReturn(page);

        // when
        var result = sectionInfoMessageController.getMessages(5L, PageRequest.of(0, 20));

        // then
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTotalElements());
        assertEquals("Section Leader", result.getBody().getContent().get(0).getAuthorName());
    }
}
