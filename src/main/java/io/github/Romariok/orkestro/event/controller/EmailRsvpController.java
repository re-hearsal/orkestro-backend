package io.github.Romariok.orkestro.event.controller;

import io.github.Romariok.orkestro.event.service.EmailRsvpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/rsvp/email")
@RequiredArgsConstructor
public class EmailRsvpController {

    private final EmailRsvpService emailRsvpService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmAttendance(@RequestParam() String token) {
        String message = emailRsvpService.confirmAttendanceByToken(token);
        String html = """
                <!doctype html>
                <html lang="ru">
                <head><meta charset="UTF-8"><title>RSVP</title></head>
                <body style="font-family: Arial, sans-serif; padding: 24px;">
                  <h2>%s</h2>
                </body>
                </html>
                """.formatted(message);
        return ResponseEntity.ok(html);
    }
}
