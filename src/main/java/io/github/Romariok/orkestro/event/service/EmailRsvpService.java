package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailRsvpService {

    private final EmailRsvpTokenService tokenService;
    private final EventParticipantRepository eventParticipantRepository;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    @Transactional
    public String confirmAttendanceByToken(String token) {
        if (token == null || token.isBlank()) {
            return messageSource.getMessage("notification.email.rsvp.invalid-link", null, Locale.forLanguageTag("ru"));
        }

        try {
            EmailRsvpTokenService.ParsedEmailRsvpToken parsed = tokenService.parseToken(token);
            Locale locale = resolveLocale(parsed.userId());
            EventParticipant participant = eventParticipantRepository
                    .findByEventIdAndUserId(parsed.eventId(), parsed.userId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Event participant not found for event " + parsed.eventId() + " and user " + parsed.userId()));

            boolean accepted = parsed.accepted();
            participant.setRsvpStatus(accepted ? EventRsvpStatus.ACCEPTED : EventRsvpStatus.DECLINED);
            participant.setRsvpAt(Instant.now());
            eventParticipantRepository.save(participant);

            return accepted
                    ? messageSource.getMessage("notification.email.rsvp.accepted", null, locale)
                    : messageSource.getMessage("notification.email.rsvp.declined", null, locale);
        } catch (RuntimeException ex) {
            return messageSource.getMessage("notification.email.rsvp.expired", null, Locale.forLanguageTag("ru"));
        }
    }

    private Locale resolveLocale(Long userId) {
        UserLanguageType language = userRepository.findById(userId)
                .map(user -> user.getPreferredLanguage())
                .orElse(UserLanguageType.RU);
        return language == UserLanguageType.EN ? Locale.ENGLISH : Locale.forLanguageTag("ru");
    }
}
