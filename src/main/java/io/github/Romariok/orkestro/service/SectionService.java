package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dto.section.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.dto.section.SectionDTO;
import io.github.Romariok.orkestro.mapper.SectionMapper;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.section.Section;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SectionRepository;
import io.github.Romariok.orkestro.repository.SectionUserRepository;
import io.github.Romariok.orkestro.repository.TaskRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final OrganizationRepository organizationRepository;
    private final SectionUserRepository sectionUserRepository;
    private final RoleRepository roleRepository;
    private final TaskRepository taskRepository;
    private final SectionMapper sectionMapper;

    /**
     * Создать секцию верхнего уровня внутри организации.
     * Доступно только обладателям SECTION_CREATE в контексте организации.
     */
    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':SECTION_CREATE')")
    public SectionDTO createSectionInOrganization(Long organizationId, SectionCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Section name must not be blank");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

        String normalizedName = request.getName().trim();

        if (sectionRepository.existsByOrganizationIdAndParentSectionIdIsNullAndName(
                organization.getId(), normalizedName)) {
            throw new BusinessException(
                    "Section with name '" + normalizedName + "' already exists in organization " + organizationId);
        }

        Section section = new Section();
        section.setName(normalizedName);
        section.setDescription(request.getDescription());
        section.setOrganizationId(organization.getId());
        section.setParentSectionId(null);

        Section saved = sectionRepository.save(section);
        return sectionMapper.toDto(saved);
    }

    /**
     * Создать секцию внутри другой секции.
     * Доступно только обладателям SECTION_CREATE в контексте родительской секции.
     */
    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_SECTION:' + #parentSectionId + ':SECTION_CREATE')")
    public SectionDTO createSectionInSection(Long parentSectionId, SectionCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Section name must not be blank");
        }

        Section parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found: " + parentSectionId));

        String normalizedName = request.getName().trim();

        if (sectionRepository.existsByOrganizationIdAndParentSectionIdAndName(
                parent.getOrganizationId(), parent.getId(), normalizedName)) {
            throw new BusinessException(
                    "Section with name '" + normalizedName + "' already exists under section " + parentSectionId);
        }

        Section section = new Section();
        section.setName(normalizedName);
        section.setDescription(request.getDescription());
        section.setOrganizationId(parent.getOrganizationId());
        section.setParentSectionId(parent.getId());

        Section saved = sectionRepository.save(section);
        return sectionMapper.toDto(saved);
    }

    /**
     * Удалить секцию и все вложенные секции.
     * Доступно только обладателям SECTION_DELETE в контексте удаляемой секции.
     */
    @Transactional
    @PreAuthorize("hasAuthority('CTX_PERM_SECTION:' + #sectionId + ':SECTION_DELETE')")
    public void deleteSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        List<Long> idsToDelete = new ArrayList<>();
        collectSubtreeSectionIds(sectionId, idsToDelete);

        // Чистим зависимости, которые ссылаются на секции
        for (Long id : idsToDelete) {
            // Участники секций
            sectionUserRepository.deleteBySectionId(id);

            // Таски, привязанные к секции
            taskRepository.findBySectionId(id).forEach(taskRepository::delete);

            // Роли секций и связанные user_role / role_permission (каскадом по FK)
            List<Role> sectionRoles = roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, id);
            if (!sectionRoles.isEmpty()) {
                roleRepository.deleteAll(sectionRoles);
            }
        }

        // Удаляем секции начиная с листьев (порядок уже обеспечен
        // collectSubtreeSectionIds)
        sectionRepository.deleteAllById(idsToDelete);
    }

    private void collectSubtreeSectionIds(Long rootId, List<Long> accumulator) {
        List<Section> children = sectionRepository.findByParentSectionId(rootId);
        for (Section child : children) {
            collectSubtreeSectionIds(child.getId(), accumulator);
        }
        accumulator.add(rootId);
    }
}
