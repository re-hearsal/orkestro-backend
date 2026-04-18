package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventCommentCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventPageDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EventCommentControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventCommentController eventCommentController;

    @Test
    void createEventComment_returnsCreated() {
        EventCommentDTO response = EventCommentDTO.builder()
                .id(1L)
                .authorUserId(42L)
                .authorName("Leader")
                .text("Weekly progress looks good")
                .createdAt(Instant.parse("2026-02-27T10:00:00Z"))
                .build();
        when(eventService.createEventComment(eq(1L), eq(10L), any(EventCommentCreateRequestDTO.class)))
                .thenReturn(response);

        var result = eventCommentController.createEventComment(
                1L,
                10L,
                EventCommentCreateRequestDTO.builder().text("Weekly progress looks good").build());

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Leader", result.getBody().getAuthorName());
    }

    @Test
    void getEventComments_returnsPagedResponse() {
        EventCommentsByEventPageDTO response = EventCommentsByEventPageDTO.builder()
                .page(0)
                .size(2)
                .totalElements(3)
                .totalPages(2)
                .first(true)
                .last(false)
                .content(List.of())
                .build();
        when(eventService.getEventCommentsByEventIds(eq(1L), eq(List.of(11L, 12L, 13L)), eq(PageRequest.of(0, 2))))
                .thenReturn(response);

        var result = eventCommentController.getEventComments(1L, List.of(11L, 12L, 13L), PageRequest.of(0, 2));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(3, result.getBody().getTotalElements());
        assertEquals(2, result.getBody().getTotalPages());
    }

    @Test
    void deleteEventComment_returnsNoContent() {
        var result = eventCommentController.deleteEventComment(1L, 10L, 5L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
