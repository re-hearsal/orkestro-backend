package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.service.EmailRsvpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/events/rsvp/email")
@RequiredArgsConstructor
@Tag(name = "Events - RSVP", description = "API для подтверждения посещения событий по email")
public class EmailRsvpController {

    private final EmailRsvpService emailRsvpService;

    @Operation(
            summary = "Подтвердить посещение по email",
            description = "Подтверждает посещение события по токену из email. Возвращает HTML-страницу с результатом."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Подтверждение успешно",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный или просроченный токен",
                    content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)
            )
    })
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmAttendance(@Parameter(description = "Токен подтверждения из email", required = true) @RequestParam() String token) {
        String message = emailRsvpService.confirmAttendanceByToken(token);
        String html = "<!doctype html>"
                + "<html lang=\"ru\">"
                + "<head><meta charset=\"UTF-8\"><title>RSVP</title></head>"
                + "<body style=\"font-family: Arial, sans-serif; padding: 24px;\">"
                + "  <h2>" + message + "</h2>"
                + "</body>"
                + "</html>";
        return ResponseEntity.ok(html);
    }
}
