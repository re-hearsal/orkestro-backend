package io.github.Romariok.orkestro.event.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.service.EmailRsvpService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class EmailRsvpControllerTest {

    @Mock
    private EmailRsvpService emailRsvpService;

    @InjectMocks
    private EmailRsvpController emailRsvpController;

    @Test
    void confirmAttendance_validToken_returnsOkWithHtml() {
        when(emailRsvpService.confirmAttendanceByToken(eq("valid-token-123")))
                .thenReturn("Посещение подтверждено!");

        var result = emailRsvpController.confirmAttendance("valid-token-123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().contains("Посещение подтверждено!"));
        assertTrue(result.getBody().contains("<!doctype html>"));
    }

    @Test
    void confirmAttendance_returnsHtmlContainingMessage() {
        String message = "Спасибо за подтверждение участия";
        when(emailRsvpService.confirmAttendanceByToken(eq("abc-token")))
                .thenReturn(message);

        var result = emailRsvpController.confirmAttendance("abc-token");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().contains(message));
        assertTrue(result.getBody().contains("<h2>"));
    }
}
