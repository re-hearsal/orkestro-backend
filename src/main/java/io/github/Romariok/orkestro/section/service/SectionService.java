package io.github.Romariok.orkestro.section.service;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.dto.SectionMemberDTO;
import io.github.Romariok.orkestro.section.mapper.SectionMapper;
import io.github.Romariok.orkestro.section.mapper.SectionMemberMapper;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.section.specification.SectionUserSpecifications;
import io.github.Romariok.orkestro.event.repository.EventSectionRepository;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.RolePermission;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.UserRoleId;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.exception.SectionDepthExceededException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final UserRoleRepository userRoleRepository;
    private final TaskRepository taskRepository;
    private final EventSectionRepository eventSectionRepository;
    private final SectionMapper sectionMapper;
    private final SectionMemberMapper sectionMemberMapper;
    private final RolePermissionRepository rolePermissionRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
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
        ensureCreatorMembershipAndLeaderRole(saved.getId());
        return sectionMapper.toDto(saved);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.isSectionMember(#parentSectionId)")
    public SectionDTO createSectionInSection(Long parentSectionId, SectionCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Section name must not be blank");
        }

        Section parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found: " + parentSectionId));

        checkSectionDepth(parent);

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
        ensureCreatorMembershipAndLeaderRole(saved.getId());
        return sectionMapper.toDto(saved);
    }

    private void checkSectionDepth(Section parent) {
        int depth = 0;
        Section current = parent;
        while (current != null) {
            depth++;
            if (depth >= 10) {
                throw new SectionDepthExceededException("Maximum section nesting depth of 10 exceeded");
            }
            if (current.getParentSectionId() == null) {
                break;
            }
            current = sectionRepository.findById(current.getParentSectionId()).orElse(null);
        }
    }

    private void ensureCreatorMembershipAndLeaderRole(Long sectionId) {
        Long userId = securityUtils.getCurrentUserId();

        // ensure member record
        if (sectionUserRepository.findBySectionIdAndUserId(sectionId, userId).isEmpty()) {
            SectionUser su = new SectionUser();
            su.setSectionId(sectionId);
            su.setUserId(userId);
            su.setJoinedAt(Instant.now());
            sectionUserRepository.save(su);
        }

        // portability: keep roles consistent with current composition
        syncSectionLeadershipRoles(sectionId);
    }

    /**
     * Brings section leadership roles to a consistent state relative to the current membership.
     * <p>
     * Invariants enforced:
     * - if there is at least 1 member, the section must have exactly one {@code Leader}
     *   (the assignment must point to a current section member)
     * - if there are 2+ members, the section must have exactly one {@code Co-leader},
     *   the assignee must be a current section member and must not be the same as {@code Leader}
     * - if there is exactly 1 member, there must be no {@code Co-leader} assignment
     * <p>
     * Priority is to keep the current leader/co-leader assignee if they are still a section member.
     * If the assignment is missing/invalid (assigned to a non-member or equals a forbidden assignee),
     * a default candidate is selected from the current membership.
     * <p>
     * For each role, at most one assignment is kept: extra assignments are removed.
     * If the role itself (Leader/Co-leader) does not exist in the database, the corresponding
     * part of the synchronization is skipped.
     */
    private void syncSectionLeadershipRoles(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            return;
        }

        List<SectionUser> members = sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId);
        if (members.isEmpty()) {
            return;
        }

        List<Long> memberUserIds = members.stream().map(SectionUser::getUserId).toList();

        Long leaderUserId = roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Leader")
                .map(leaderRole -> ensureSingleRoleAssignee(leaderRole.getId(), memberUserIds, memberUserIds.getFirst(),
                        null))
                .orElse(null);

        roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Co-leader")
                .ifPresent(coLeaderRole -> {
                    if (memberUserIds.size() < 2) {
                        // if only one member, co-leader must not exist
                        userRoleRepository.deleteByRoleId(coLeaderRole.getId());
                        return;
                    }

                    Long effectiveLeaderUserId = leaderUserId != null ? leaderUserId : memberUserIds.getFirst();
                    Long defaultCoLeader = memberUserIds.stream()
                            .filter(id -> !id.equals(effectiveLeaderUserId))
                            .findFirst()
                            .orElse(memberUserIds.get(1));

                    ensureSingleRoleAssignee(coLeaderRole.getId(), memberUserIds, defaultCoLeader,
                            effectiveLeaderUserId);
                });
    }

    private Long resolveCurrentAssigneeUserId(Long roleId, List<Long> memberUserIds) {
        List<UserRole> mappings = userRoleRepository.findByRoleId(roleId);
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }
        for (UserRole mapping : mappings) {
            if (mapping != null && mapping.getUserId() != null && memberUserIds.contains(mapping.getUserId())) {
                return mapping.getUserId();
            }
        }
        return null;
    }

    /**
     * Ensures exactly one assignment of {@code roleId} to one of the section members.
     * <p>
     * Algorithm:
     * - keep the current assignee if it is valid (in membership and not equal to {@code forbiddenUserId})
     * - otherwise use {@code defaultUserId} (or the first suitable member if needed)
     * - remove all existing assignments for the role and create one correct assignment (if a candidate is selected)
     *
     * @return the actual assigned user id (or {@code null} if no assignment is created)
     */
    private Long ensureSingleRoleAssignee(
            Long roleId,
            List<Long> memberUserIds,
            Long defaultUserId,
            Long forbiddenUserId) {
        Long selected = resolveCurrentAssigneeUserId(roleId, memberUserIds);
        if (forbiddenUserId != null && forbiddenUserId.equals(selected)) {
            selected = null;
        }
        if (selected == null) {
            selected = defaultUserId;
            if (forbiddenUserId != null && forbiddenUserId.equals(selected)) {
                selected = memberUserIds.stream().filter(id -> !id.equals(forbiddenUserId)).findFirst().orElse(null);
            }
        }

        userRoleRepository.deleteByRoleId(roleId);
        if (selected != null) {
            userRoleRepository.save(UserRole.builder()
                    .userId(selected)
                    .roleId(roleId)
                    .build());
        }
        return selected;
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


    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_DELETE')")
    public void deleteSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        deleteSectionCascade(sectionId);
    }

    private void deleteSectionCascade(Long sectionId) {
        List<Long> idsToDelete = new ArrayList<>();
        collectSubtreeSectionIds(sectionId, idsToDelete);

        for (Long id : idsToDelete) {
            sectionUserRepository.deleteBySectionId(id);

            taskRepository.findBySectionId(id).forEach(taskRepository::delete);

            eventSectionRepository.deleteBySectionId(id);

            List<Role> sectionRoles = roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, id);
            if (!sectionRoles.isEmpty()) {
                roleRepository.deleteAll(sectionRoles);
            }
        }

        sectionRepository.deleteAllById(idsToDelete);
    }

    /**
     * Adds a user to a section.
     * The user must be eligible at the level above:
     * - for a root section (without parent_section_id) the user must be an accepted member of the organization;
     * - for a nested section the user must be a member of the parent section.
     * Available only to users with SECTION_MEMBER_ADD permission in the context of the section.
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
        syncSectionLeadershipRoles(sectionId);
    }

    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasSectionPermission(#sectionId, 'SECTION_MEMBER_REMOVE')")
    public void removeUserFromSection(Long sectionId, Long userId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        Long currentUserId = securityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(userId)) {
            throw new IllegalArgumentException(
                    "Cannot remove yourself from section using this endpoint. Use leave endpoint instead.");
        }

        if (sectionUserRepository.findBySectionIdAndUserId(sectionId, userId).isEmpty()) {
            return;
        }

        List<Long> subtree = new ArrayList<>();
        collectSubtreeSectionIds(sectionId, subtree);
        sectionUserRepository.deleteBySectionIdInAndUserId(subtree, userId);
        cleanupUserRolesForUserInSections(userId, subtree);
        reconcileSectionsAfterMemberChange(subtree);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@organizationPermissionChecker.isSectionMember(#sectionId)")
    public Page<SectionMemberDTO> searchMembers(
            Long sectionId,
            String query,
            List<Long> roleIds,
            List<Long> instrumentIds,
            Pageable pageable) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        Pageable mappedPageable = mapSectionMemberSort(pageable);

        Specification<SectionUser> spec = Specification.where(
                SectionUserSpecifications.isSectionMember(sectionId))
                .and(SectionUserSpecifications.userNameOrUsernameContainsIgnoreCase(query))
                .and(SectionUserSpecifications.userHasAnySectionRole(sectionId, roleIds))
                .and(SectionUserSpecifications.userHasAnyInstrument(instrumentIds));

        return sectionUserRepository.findAll(spec, mappedPageable).map(sectionMemberMapper::toDto);
    }

    private Pageable mapSectionMemberSort(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        Sort sort = pageable.getSort();
        if (sort == null || sort.isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> mapped = new ArrayList<>();
        for (Sort.Order order : sort) {
            String prop = order.getProperty();
            String mappedProp = switch (prop) {
                case "joinedAt" -> "joinedAt";
                case "id" -> "user.id";
                case "username" -> "user.username";
                case "name" -> "user.name";
                default -> throw new IllegalArgumentException("Unsupported sort property: " + prop);
            };
            Sort.Order mappedOrder = new Sort.Order(order.getDirection(), mappedProp)
                    .with(order.getNullHandling());
            if (order.isIgnoreCase()) {
                mappedOrder = mappedOrder.ignoreCase();
            }
            mapped.add(mappedOrder);
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mapped));
    }

    /**
     * Leaves the section as the current user.
     * <p>
     * Exit rules:
     * - if the current user has {@code Leader} role and there are other members (2+ total), leaving is forbidden
     *   (BusinessException)
     * - if the current user has {@code Co-leader} role and there are other members besides the leader (3+ total),
     *   leaving is forbidden (BusinessException)
     * - if there are exactly 2 members and the current user is {@code Co-leader}, leaving is allowed
     * - if the current user is {@code Leader} and is the last member, leaving is allowed:
     *   after membership removal the section will be deleted as empty
     * <p>
     * Actions when leaving is allowed:
     * - removes the user's membership in the section and all nested sections (subtree)
     * - cleans up the user's technical roles within those sections
     * - reconciles sections state: deletes empty sections and syncs leadership roles in non-empty sections
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.isSectionMember(#sectionId)")
    public void leaveCurrentSection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new EntityNotFoundException("Section not found: " + sectionId);
        }

        Long userId = securityUtils.getCurrentUserId();

        List<SectionUser> members = sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId);
        int memberCount = members.size();
        if (memberCount > 0) {
            boolean isLeader = roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Leader")
                    .map(role -> userRoleRepository
                            .existsById(UserRoleId.builder().userId(userId).roleId(role.getId()).build()))
                    .orElse(false);

            boolean isCoLeader = roleRepository
                    .findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Co-leader")
                    .map(role -> userRoleRepository
                            .existsById(UserRoleId.builder().userId(userId).roleId(role.getId()).build()))
                    .orElse(false);

            if (isLeader && memberCount > 1) {
                throw new BusinessException("Leader cannot leave section while other members exist");
            }
            if (isCoLeader && memberCount > 2) {
                throw new BusinessException("Co-leader cannot leave section while other non-leader members exist");
            }
        }

        List<Long> subtree = new ArrayList<>();
        collectSubtreeSectionIds(sectionId, subtree);
        sectionUserRepository.deleteBySectionIdInAndUserId(subtree, userId);
        cleanupUserRolesForUserInSections(userId, subtree);
        reconcileSectionsAfterMemberChange(subtree);
    }

    private void cleanupUserRolesForUserInSections(Long userId, List<Long> sectionIds) {
        List<Role> roles = roleRepository.findByScopeAndSectionIdIn(RoleScopeType.SECTION, sectionIds);
        if (roles.isEmpty()) {
            return;
        }
        List<Long> roleIds = roles.stream().map(Role::getId).toList();
        userRoleRepository.deleteByUserIdAndRoleIdIn(userId, roleIds);
    }

    private void reconcileSectionsAfterMemberChange(List<Long> sectionIdsPostOrder) {
        for (Long sectionId : sectionIdsPostOrder) {
            if (!sectionRepository.existsById(sectionId)) {
                continue;
            }
            long members = sectionUserRepository.countBySectionId(sectionId);
            if (members == 0L) {
                deleteSectionCascade(sectionId);
                continue;
            }
            syncSectionLeadershipRoles(sectionId);
        }
    }

    private void collectSubtreeSectionIds(Long rootId, List<Long> accumulator) {
        List<Section> children = sectionRepository.findByParentSectionId(rootId);
        for (Section child : children) {
            collectSubtreeSectionIds(child.getId(), accumulator);
        }
        accumulator.add(rootId);
    }
}
