package io.github.Romariok.orkestro.event.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateDTO;
import io.github.Romariok.orkestro.event.models.EventDescriptionTemplate;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventDescriptionTemplateRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventDescriptionTemplateServiceTest {

    @Mock
    private EventDescriptionTemplateRepository templateRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private EventDescriptionTemplateService eventDescriptionTemplateService;

    @Test
    void createTemplate_success_savesAndReturnsDto() {
        Long organizationId = 1L;
        Long currentUserId = 42L;
        EventDescriptionTemplateCreateRequestDTO request = new EventDescriptionTemplateCreateRequestDTO(
                "Rehearsal Template", EventType.REHEARSAL, "Standard rehearsal description");

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

        EventDescriptionTemplate saved = EventDescriptionTemplate.builder()
                .id(10L)
                .organizationId(organizationId)
                .eventType(EventType.REHEARSAL)
                .title("Rehearsal Template")
                .content("Standard rehearsal description")
                .createdByUserId(currentUserId)
                .createdAt(Instant.now())
                .build();
        when(templateRepository.save(any(EventDescriptionTemplate.class))).thenReturn(saved);

        EventDescriptionTemplateDTO result = eventDescriptionTemplateService.createTemplate(organizationId, request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals(EventType.REHEARSAL, result.getEventType());

        ArgumentCaptor<EventDescriptionTemplate> captor = ArgumentCaptor.forClass(EventDescriptionTemplate.class);
        verify(templateRepository).save(captor.capture());
        EventDescriptionTemplate toSave = captor.getValue();
        assertEquals(organizationId, toSave.getOrganizationId());
        assertEquals(currentUserId, toSave.getCreatedByUserId());
    }

    @Test
    void listTemplates_noFilter_returnsAllTemplates() {
        Long organizationId = 1L;

        EventDescriptionTemplate t1 = EventDescriptionTemplate.builder()
                .id(1L)
                .organizationId(organizationId)
                .eventType(EventType.REHEARSAL)
                .title("T1")
                .content("C1")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();
        EventDescriptionTemplate t2 = EventDescriptionTemplate.builder()
                .id(2L)
                .organizationId(organizationId)
                .eventType(EventType.CONCERT)
                .title("T2")
                .content("C2")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findByOrganizationId(organizationId)).thenReturn(List.of(t1, t2));

        List<EventDescriptionTemplateDTO> result = eventDescriptionTemplateService.listTemplates(organizationId, null);

        assertEquals(2, result.size());
        verify(templateRepository).findByOrganizationId(organizationId);
    }

    @Test
    void listTemplates_withEventTypeFilter_returnsFilteredTemplates() {
        Long organizationId = 1L;

        EventDescriptionTemplate t1 = EventDescriptionTemplate.builder()
                .id(1L)
                .organizationId(organizationId)
                .eventType(EventType.REHEARSAL)
                .title("Rehearsal T")
                .content("Content")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findByOrganizationIdAndEventType(organizationId, EventType.REHEARSAL))
                .thenReturn(List.of(t1));

        List<EventDescriptionTemplateDTO> result = eventDescriptionTemplateService.listTemplates(organizationId, EventType.REHEARSAL);

        assertEquals(1, result.size());
        assertEquals(EventType.REHEARSAL, result.getFirst().getEventType());
        verify(templateRepository).findByOrganizationIdAndEventType(organizationId, EventType.REHEARSAL);
    }

    @Test
    void updateTemplate_success_updatesAndReturnsDto() {
        Long organizationId = 1L;
        Long templateId = 10L;
        EventDescriptionTemplateCreateRequestDTO request = new EventDescriptionTemplateCreateRequestDTO(
                "Updated Title", EventType.CONCERT, "Updated content");

        EventDescriptionTemplate existing = EventDescriptionTemplate.builder()
                .id(templateId)
                .organizationId(organizationId)
                .eventType(EventType.REHEARSAL)
                .title("Old Title")
                .content("Old content")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any(EventDescriptionTemplate.class))).thenReturn(existing);

        EventDescriptionTemplateDTO result = eventDescriptionTemplateService.updateTemplate(
                organizationId, templateId, request);

        assertNotNull(result);
        assertEquals("Updated Title", existing.getTitle());
        assertEquals(EventType.CONCERT, existing.getEventType());
        assertEquals("Updated content", existing.getContent());
        verify(templateRepository).save(existing);
    }

    @Test
    void updateTemplate_notFound_throwsEntityNotFoundException() {
        Long organizationId = 1L;
        Long templateId = 99L;
        EventDescriptionTemplateCreateRequestDTO request = new EventDescriptionTemplateCreateRequestDTO(
                "Title", EventType.REHEARSAL, "Content");

        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> eventDescriptionTemplateService.updateTemplate(organizationId, templateId, request));

        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_wrongOrganization_throwsBusinessException() {
        Long organizationId = 1L;
        Long templateId = 10L;
        EventDescriptionTemplateCreateRequestDTO request = new EventDescriptionTemplateCreateRequestDTO(
                "Title", EventType.REHEARSAL, "Content");

        EventDescriptionTemplate existing = EventDescriptionTemplate.builder()
                .id(templateId)
                .organizationId(999L) // different org
                .eventType(EventType.REHEARSAL)
                .title("Old")
                .content("Old content")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existing));

        assertThrows(
                BusinessException.class,
                () -> eventDescriptionTemplateService.updateTemplate(organizationId, templateId, request));

        verify(templateRepository, never()).save(any());
    }

    @Test
    void deleteTemplate_success_deletesTemplate() {
        Long organizationId = 1L;
        Long templateId = 10L;

        EventDescriptionTemplate existing = EventDescriptionTemplate.builder()
                .id(templateId)
                .organizationId(organizationId)
                .eventType(EventType.REHEARSAL)
                .title("Title")
                .content("Content")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existing));

        eventDescriptionTemplateService.deleteTemplate(organizationId, templateId);

        verify(templateRepository).delete(existing);
    }

    @Test
    void deleteTemplate_notFound_throwsEntityNotFoundException() {
        Long organizationId = 1L;
        Long templateId = 99L;

        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> eventDescriptionTemplateService.deleteTemplate(organizationId, templateId));

        verify(templateRepository, never()).delete(any());
    }

    @Test
    void deleteTemplate_wrongOrganization_throwsBusinessException() {
        Long organizationId = 1L;
        Long templateId = 10L;

        EventDescriptionTemplate existing = EventDescriptionTemplate.builder()
                .id(templateId)
                .organizationId(999L) // different org
                .eventType(EventType.REHEARSAL)
                .title("Title")
                .content("Content")
                .createdByUserId(1L)
                .createdAt(Instant.now())
                .build();

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existing));

        assertThrows(
                BusinessException.class,
                () -> eventDescriptionTemplateService.deleteTemplate(organizationId, templateId));

        verify(templateRepository, never()).delete(any());
    }
}
