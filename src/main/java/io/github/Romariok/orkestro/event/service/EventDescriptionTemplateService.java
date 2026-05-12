package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDescriptionTemplateDTO;
import io.github.Romariok.orkestro.event.models.EventDescriptionTemplate;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventDescriptionTemplateRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventDescriptionTemplateService {

    private final EventDescriptionTemplateRepository templateRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_MANAGE_DESCRIPTIONS')")
    public EventDescriptionTemplateDTO createTemplate(
            Long organizationId, EventDescriptionTemplateCreateRequestDTO request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        EventDescriptionTemplate template = EventDescriptionTemplate.builder()
                .organizationId(organizationId)
                .eventType(request.getEventType())
                .title(request.getTitle().trim())
                .content(request.getContent())
                .createdByUserId(currentUserId)
                .build();

        return toDTO(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_MANAGE_DESCRIPTIONS')")
    public List<EventDescriptionTemplateDTO> listTemplates(Long organizationId, EventType eventType) {
        List<EventDescriptionTemplate> templates = eventType != null
                ? templateRepository.findByOrganizationIdAndEventType(organizationId, eventType)
                : templateRepository.findByOrganizationId(organizationId);

        return templates.stream().map(this::toDTO).toList();
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_MANAGE_DESCRIPTIONS')")
    public EventDescriptionTemplateDTO updateTemplate(
            Long organizationId, Long templateId, EventDescriptionTemplateCreateRequestDTO request) {
        EventDescriptionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + templateId));

        if (!template.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Template " + templateId + " does not belong to organization " + organizationId);
        }

        template.setTitle(request.getTitle().trim());
        template.setEventType(request.getEventType());
        template.setContent(request.getContent());

        return toDTO(templateRepository.save(template));
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_MANAGE_DESCRIPTIONS')")
    public void deleteTemplate(Long organizationId, Long templateId) {
        EventDescriptionTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template not found: " + templateId));

        if (!template.getOrganizationId().equals(organizationId)) {
            throw new BusinessException("Template " + templateId + " does not belong to organization " + organizationId);
        }

        templateRepository.delete(template);
    }

    private EventDescriptionTemplateDTO toDTO(EventDescriptionTemplate template) {
        return EventDescriptionTemplateDTO.builder()
                .id(template.getId())
                .organizationId(template.getOrganizationId())
                .eventType(template.getEventType())
                .title(template.getTitle())
                .content(template.getContent())
                .createdByUserId(template.getCreatedByUserId())
                .createdAt(template.getCreatedAt())
                .build();
    }
}
