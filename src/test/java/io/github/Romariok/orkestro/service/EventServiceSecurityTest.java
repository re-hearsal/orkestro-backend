package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.service.EventService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class EventServiceSecurityTest {

    @Test
    void createEventInOrganization_hasNoPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EventService.class.getMethod("createEventInOrganization", Long.class,
                EventCreateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals(null, preAuthorize);
    }

    @Test
    void updateEvent_hasNoPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = EventService.class.getMethod("updateEvent", Long.class, EventUpdateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals(null, preAuthorize);
    }

    @Test
    void deleteEvent_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = EventService.class.getMethod("deleteEvent", Long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
                        + "or hasAuthority('CTX_PERM_ORG:' + "
                        + "@eventRepository.findById(#eventId).orElse(null)?.organizationId + ':EVENT_DELETION')",
                expr);
    }
}
