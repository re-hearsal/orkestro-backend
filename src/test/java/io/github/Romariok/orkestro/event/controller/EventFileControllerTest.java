package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventFileAttachRequestDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class EventFileControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventFileController eventFileController;

    @Test
    void addFileToEvent_returnsCreated() {
        EventDTO response = EventDTO.builder()
                .id(10L)
                .organizationId(1L)
                .title("Concert")
                .eventType(EventType.CONCERT)
                .fileIds(List.of(100L))
                .build();
        when(eventService.attachFileToEvent(eq(1L), eq(10L), any(MultipartFile.class))).thenReturn(response);

        MultipartFile mockFile = org.mockito.Mockito.mock(MultipartFile.class);
        EventFileAttachRequestDTO request = EventFileAttachRequestDTO.builder()
                .file(mockFile)
                .build();

        var result = eventFileController.addFileToEvent(1L, 10L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertEquals(1, result.getBody().getFileIds().size());
    }

    @Test
    void deleteFileFromEvent_returnsOk() {
        EventDTO response = EventDTO.builder()
                .id(10L)
                .organizationId(1L)
                .title("Concert")
                .fileIds(List.of())
                .build();
        when(eventService.deleteEventFile(eq(1L), eq(10L), eq(100L))).thenReturn(response);

        var result = eventFileController.deleteFileFromEvent(1L, 10L, 100L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertEquals(0, result.getBody().getFileIds().size());
    }
}
