package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventDescriptionTemplateService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventDescriptionTemplateControllerTest {

    @Mock
    private EventDescriptionTemplateService templateService;

    @InjectMocks
    private EventDescriptionTemplateController eventDescriptionTemplateController;

    @Test
    void createTemplate_returnsCreated() {
        EventDescriptionTemplateDTO response = EventDescriptionTemplateDTO.builder()
                .id(1L)
                .organizationId(10L)
                .eventType(EventType.REHEARSAL)
                .title("Standard Rehearsal")
                .content("Today we practice...")
                .createdByUserId(42L)
                .createdAt(Instant.parse("2026-03-01T10:00:00Z"))
                .build();
        when(templateService.createTemplate(eq(10L), any(EventDescriptionTemplateCreateRequestDTO.class)))
                .thenReturn(response);

        EventDescriptionTemplateCreateRequestDTO request = EventDescriptionTemplateCreateRequestDTO.builder()
                .title("Standard Rehearsal")
                .eventType(EventType.REHEARSAL)
                .content("Today we practice...")
                .build();

        var result = eventDescriptionTemplateController.createTemplate(10L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Standard Rehearsal", result.getBody().getTitle());
    }

    @Test
    void listTemplates_returnsOk() {
        List<EventDescriptionTemplateDTO> templates = List.of(
                EventDescriptionTemplateDTO.builder().id(1L).organizationId(10L).eventType(EventType.REHEARSAL).title("Rehearsal Template").build(),
                EventDescriptionTemplateDTO.builder().id(2L).organizationId(10L).eventType(EventType.CONCERT).title("Concert Template").build()
        );
        when(templateService.listTemplates(eq(10L), eq(EventType.REHEARSAL)))
                .thenReturn(List.of(templates.get(0)));

        var result = eventDescriptionTemplateController.listTemplates(10L, EventType.REHEARSAL);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Rehearsal Template", result.getBody().get(0).getTitle());
    }

    @Test
    void listTemplates_withNullEventType_returnsAllTemplates() {
        List<EventDescriptionTemplateDTO> templates = List.of(
                EventDescriptionTemplateDTO.builder().id(1L).title("Rehearsal Template").build(),
                EventDescriptionTemplateDTO.builder().id(2L).title("Concert Template").build()
        );
        when(templateService.listTemplates(eq(10L), eq(null))).thenReturn(templates);

        var result = eventDescriptionTemplateController.listTemplates(10L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
    }

    @Test
    void updateTemplate_returnsOk() {
        EventDescriptionTemplateDTO response = EventDescriptionTemplateDTO.builder()
                .id(1L)
                .organizationId(10L)
                .eventType(EventType.CONCERT)
                .title("Updated Concert Template")
                .content("Updated content...")
                .build();
        when(templateService.updateTemplate(eq(10L), eq(1L), any(EventDescriptionTemplateCreateRequestDTO.class)))
                .thenReturn(response);

        EventDescriptionTemplateCreateRequestDTO request = EventDescriptionTemplateCreateRequestDTO.builder()
                .title("Updated Concert Template")
                .eventType(EventType.CONCERT)
                .content("Updated content...")
                .build();

        var result = eventDescriptionTemplateController.updateTemplate(10L, 1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Updated Concert Template", result.getBody().getTitle());
    }

    @Test
    void deleteTemplate_returnsNoContent() {
        doNothing().when(templateService).deleteTemplate(10L, 1L);

        var result = eventDescriptionTemplateController.deleteTemplate(10L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
