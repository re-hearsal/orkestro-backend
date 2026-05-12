package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventCommentCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventPageDTO;
import io.github.Romariok.orkestro.event.dto.EventFeedbackRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventFeedbackRowDTO;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.service.EventService;
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

    @Test
    void getEventFeedback_returnsOkWithPage() {
        EventFeedbackRowDTO row = EventFeedbackRowDTO.builder()
                .commentId(1L)
                .commentText("Excellent concert")
                .rating(5)
                .commentCreatedAt(Instant.parse("2026-03-01T10:00:00Z"))
                .authorUserId(42L)
                .authorName("Alice")
                .eventId(20L)
                .eventTitle("Spring Concert")
                .eventType(EventType.CONCERT)
                .build();
        Page<EventFeedbackRowDTO> page = new PageImpl<>(List.of(row));

        when(eventService.getEventFeedbackForCurrentUser(eq(1L), any(EventFeedbackRequestDTO.class), any()))
                .thenReturn(page);

        var result = eventCommentController.getEventFeedback(
                1L, new EventFeedbackRequestDTO(), PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
        assertEquals("Excellent concert", result.getBody().getContent().get(0).getCommentText());
    }

    @Test
    void getEventFeedback_withInvalidSortField_stillDelegatesAndReturnsOk() {
        EventFeedbackRequestDTO request = EventFeedbackRequestDTO.builder()
                .sortField("invalidField")
                .build();
        Page<EventFeedbackRowDTO> emptyPage = new PageImpl<>(List.of());

        when(eventService.getEventFeedbackForCurrentUser(eq(1L), any(EventFeedbackRequestDTO.class), any()))
                .thenReturn(emptyPage);

        var result = eventCommentController.getEventFeedback(1L, request, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, result.getBody().getTotalElements());
    }
}
