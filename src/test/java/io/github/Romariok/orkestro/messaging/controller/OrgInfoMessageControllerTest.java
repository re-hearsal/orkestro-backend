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
class OrgInfoMessageControllerTest {

    @Mock
    private OrgInfoMessageService orgInfoMessageService;

    @InjectMocks
    private OrgInfoMessageController orgInfoMessageController;

    private OrgInfoMessageDTO buildMessage(Long id) {
        return OrgInfoMessageDTO.builder()
                .id(id)
                .organizationId(1L)
                .authorUserId(10L)
                .authorName("Admin")
                .text("Orchestra rehearsal moved to Friday")
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void postMessage_returnsCreated() {
        OrgInfoMessageDTO created = buildMessage(1L);
        when(orgInfoMessageService.postOrgMessage(eq(1L), eq("Orchestra rehearsal moved to Friday")))
                .thenReturn(created);
        OrgInfoMessageCreateRequestDTO request = new OrgInfoMessageCreateRequestDTO();
        request.setText("Orchestra rehearsal moved to Friday");

        var result = orgInfoMessageController.postMessage(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Orchestra rehearsal moved to Friday", result.getBody().getText());
    }

    @Test
    void getMessages_returnsOk() {
        Page<OrgInfoMessageDTO> page = new PageImpl<>(List.of(buildMessage(1L), buildMessage(2L)));
        when(orgInfoMessageService.getOrgMessages(eq(1L), any())).thenReturn(page);

        var result = orgInfoMessageController.getMessages(1L, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTotalElements());
        assertEquals("Admin", result.getBody().getContent().get(0).getAuthorName());
    }
}
