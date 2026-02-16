package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailRsvpService {

    private final EmailRsvpTokenService tokenService;
    private final EventParticipantRepository eventParticipantRepository;

    @Transactional
    public String confirmAttendanceByToken(String token) {
        if (token == null || token.isBlank()) {
            return "Некорректная ссылка подтверждения.";
        }

        try {
            EmailRsvpTokenService.ParsedEmailRsvpToken parsed = tokenService.parseToken(token);
            EventParticipant participant = eventParticipantRepository
                    .findByEventIdAndUserId(parsed.eventId(), parsed.userId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Event participant not found for event " + parsed.eventId() + " and user " + parsed.userId()));

            boolean accepted = parsed.accepted();
            participant.setRsvpStatus(accepted ? EventRsvpStatus.ACCEPTED : EventRsvpStatus.DECLINED);
            participant.setRsvpAt(Instant.now());
            eventParticipantRepository.save(participant);

            return accepted
                    ? "Ваш ответ сохранен: Вы отметили, что придете."
                    : "Ваш ответ сохранен: Вы отметили, что не сможете прийти.";
        } catch (Exception ex) {
            return "Ссылка недействительна или срок ее действия истек.";
        }
    }
}
