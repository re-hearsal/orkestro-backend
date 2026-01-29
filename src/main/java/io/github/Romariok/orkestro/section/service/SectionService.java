package io.github.Romariok.orkestro.section.service;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.mapper.SectionMapper;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.RolePermission;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final SectionUserRepository sectionUserRepository;
    private final RoleRepository roleRepository;
    private final TaskRepository taskRepository;
    private final SectionMapper sectionMapper;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Создать секцию верхнего уровня внутри организации.
     * Доступно только обладателям SECTION_CREATE в контексте организации.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'SECTION_CREATE')")
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
        ensureSectionBaseRoles(saved.getId());
        return sectionMapper.toDto(saved);
    }

    /**
     * Создать секцию внутри другой секции.
     * Доступно только обладателям SECTION_CREATE в контексте родительской секции.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#parentSectionId, 'SECTION_CREATE')")
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
        ensureSectionBaseRoles(saved.getId());
        return sectionMapper.toDto(saved);
    }

    private void ensureSectionBaseRoles(Long sectionId) {
        List<Role> existing = roleRepository.findByScopeAndSectionId(
                RoleScopeType.SECTION,
                sectionId);

        boolean hasLeader = existing.stream().anyMatch(r -> "Leader".equals(r.getName()));
        boolean hasCoLeader = existing.stream().anyMatch(r -> "Co-leader".equals(r.getName()));

        if (hasLeader && hasCoLeader) {
            return;
        }

        List<Role> templates = roleRepository.findByScopeAndSystemTrue(RoleScopeType.SECTION);
        Map<String, Role> templateByName = new HashMap<>();
        for (Role template : templates) {
            templateByName.put(template.getName(), template);
        }

        List<Role> toCreate = new ArrayList<>();

        if (!hasLeader && templateByName.containsKey("Leader")) {
            Role template = templateByName.get("Leader");
            Role role = Role.builder()
                    .scope(RoleScopeType.SECTION)
                    .sectionId(sectionId)
                    .name(template.getName())
                    .system(true)
                    .createdAt(Instant.now())
                    .build();
            toCreate.add(role);
        }

        if (!hasCoLeader && templateByName.containsKey("Co-leader")) {
            Role template = templateByName.get("Co-leader");
            Role role = Role.builder()
                    .scope(RoleScopeType.SECTION)
                    .sectionId(sectionId)
                    .name(template.getName())
                    .system(true)
                    .createdAt(Instant.now())
                    .build();
            toCreate.add(role);
        }

        if (toCreate.isEmpty()) {
            return;
        }

        List<Role> created = roleRepository.saveAll(toCreate);

        // Копируем права с шаблонных ролей
        List<RolePermission> permissionsToCreate = new ArrayList<>();
        for (Role createdRole : created) {
            Role template = templateByName.get(createdRole.getName());
            if (template == null) {
                continue;
            }
            List<Permission> templatePermissions = rolePermissionRepository.findPermissionsByRoleId(template.getId());
            for (Permission permission : templatePermissions) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(createdRole.getId());
                rp.setPermissionCode(permission.getCode());
                permissionsToCreate.add(rp);
            }
        }

        if (!permissionsToCreate.isEmpty()) {
            rolePermissionRepository.saveAll(permissionsToCreate);
        }
    }

    /**
     * Удалить секцию и все вложенные секции.
     * Доступно только обладателям SECTION_DELETE в контексте удаляемой секции.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_DELETE')")
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

    /**
     * Добавить пользователя в секцию.
     * Пользователь должен быть доступен на уровне выше:
     * - для корневой секции (без parent_section_id) — быть принятым участником
     * организации;
     * - для вложенной секции — быть участником родительской секции.
     * Доступно только обладателям SECTION_MEMBER_ADD в контексте секции.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_MEMBER_ADD')")
    public void addUserToSection(Long sectionId, Long userId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found: " + sectionId));

        if (sectionUserRepository.findBySectionIdAndUserId(sectionId, userId).isPresent()) {
            return;
        }

        if (section.getParentSectionId() == null) {
            OrganizationUser membership = organizationUserRepository
                    .findByOrganizationIdAndUserId(section.getOrganizationId(), userId)
                    .orElseThrow(() -> new BusinessException(
                            "User " + userId + " is not a member of organization " + section.getOrganizationId()));

            if (membership.getStatus() != OrganizationUserStatusType.ACCEPTED) {
                throw new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + section.getOrganizationId());
            }
        } else {
            Long parentSectionId = section.getParentSectionId();
            sectionUserRepository.findBySectionIdAndUserId(parentSectionId, userId)
                    .orElseThrow(() -> new BusinessException(
                            "User " + userId + " is not a member of parent section " + parentSectionId));
        }

        SectionUser su = new SectionUser();
        su.setSectionId(sectionId);
        su.setUserId(userId);
        su.setJoinedAt(Instant.now());

        sectionUserRepository.save(su);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_MEMBER_REMOVE')")
    public void removeUserFromSection(Long sectionId, Long userId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)
                .ifPresent(sectionUserRepository::delete);
    }

    private void collectSubtreeSectionIds(Long rootId, List<Long> accumulator) {
        List<Section> children = sectionRepository.findByParentSectionId(rootId);
        for (Section child : children) {
            collectSubtreeSectionIds(child.getId(), accumulator);
        }
        accumulator.add(rootId);
    }
}
