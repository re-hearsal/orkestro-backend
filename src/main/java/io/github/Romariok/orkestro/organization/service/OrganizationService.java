package io.github.Romariok.orkestro.organization.service;

import io.github.Romariok.orkestro.organization.dto.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationLinkDTO;
import io.github.Romariok.orkestro.organization.dto.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.organization.mapper.OrganizationMapper;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationInvite;
import io.github.Romariok.orkestro.organization.models.OrgFund;
import io.github.Romariok.orkestro.organization.models.OrganizationLink;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrgFundRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationInviteRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationLinkRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.repertoire.models.Song;
import io.github.Romariok.orkestro.repertoire.repository.SongRepository;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.RolePermission;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

   private final OrganizationRepository organizationRepository;
   private final OrganizationLinkRepository organizationLinkRepository;
   private final OrganizationInviteRepository organizationInviteRepository;
   private final OrganizationUserRepository organizationUserRepository;
   private final OrgFundRepository orgFundRepository;
   private final StoredFileRepository storedFileRepository;
   private final SongRepository songRepository;
   private final RoleRepository roleRepository;
   private final UserRoleRepository userRoleRepository;
   private final OrganizationMapper organizationMapper;
   private final RolePermissionRepository rolePermissionRepository;
   private final SecurityUtils securityUtils;
   private final FileStorageService fileStorageService;
   private final FileReferenceService fileReferenceService;
   private final FileRollbackHelper fileRollbackHelper;
   private final FileLimitsProperties fileLimitsProperties;
   private final EventRepository eventRepository;
   private final TaskRepository taskRepository;
   private final SectionRepository sectionRepository;
   private final SectionUserRepository sectionUserRepository;

   @Transactional
   public OrganizationDTO createOrganization(OrganizationCreateRequestDTO request) {
      String normalizedName = request.getName() == null ? null : request.getName().trim();
      String normalizedLocation = request.getLocation() == null ? null : request.getLocation().trim();
      String normalizedDescription = request.getDescription() == null ? null : request.getDescription().trim();

      StoredFile file = null;
      try {
         if (request.getProfileImage() != null && !request.getProfileImage().isEmpty()) {
            validateProfileImageFile(request.getProfileImage());
            file = fileStorageService.uploadForCurrentUser(
                  request.getProfileImage(),
                  FileType.PHOTO);
         }

         Organization organization = Organization.builder()
               .name(normalizedName)
               .location(normalizedLocation)
               .description(normalizedDescription)
               .profileImageFileId(file != null ? file.getId() : null)
               .createdAt(Instant.now())
               .build();

         Organization saved = organizationRepository.save(organization);

         orgFundRepository.save(OrgFund.builder()
                 .organizationId(saved.getId())
                 .balance(java.math.BigDecimal.ZERO)
                 .build());

         saveLinks(saved.getId(), request.getLinks());

         // Создатель автоматически становится участником организации
         Long creatorId = securityUtils.getCurrentUserId();
         OrganizationUser creatorMembership = OrganizationUser.builder()
               .organizationId(saved.getId())
               .userId(creatorId)
               .status(OrganizationUserStatusType.ACCEPTED)
               .joinedAt(Instant.now())
               .build();
         organizationUserRepository.save(creatorMembership);

         // Первый участник становится Leader
         ensureOrganizationBaseRoles(saved.getId());
         syncOrganizationLeadershipRoles(saved.getId());

         createOrRegenerateInvite(saved.getId(), creatorId);

         return buildOrganizationDto(saved);
      } catch (RuntimeException ex) {
         fileRollbackHelper.deleteFilesSafely(file != null ? List.of(file.getId()) : List.of());
         throw ex;
      }
   }

   private void validateProfileImageFile(org.springframework.web.multipart.MultipartFile profileImage) {
      if (profileImage == null || profileImage.isEmpty() || profileImage.getSize() <= 0) {
         throw new io.github.Romariok.orkestro.utils.exception.BusinessException(
               "Organization profile image must be a non-empty image file");
      }
      String contentType = profileImage.getContentType();
      if (contentType == null || contentType.isBlank() || !contentType.startsWith("image/")) {
         throw new io.github.Romariok.orkestro.utils.exception.BusinessException(
               "Organization profile image must be an image file");
      }
   }

   /**
    * Обновить параметры организации.
    */
   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_EDIT')")
   public OrganizationDTO updateOrganization(Long organizationId, OrganizationUpdateRequestDTO request) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      if (request.getName() != null) {
         String normalized = request.getName().trim();
         if (normalized.isBlank()) {
            throw new IllegalArgumentException("Organization name must not be blank");
         }
         organization.setName(normalized);
      }
      if (request.getLocation() != null) {
         String normalized = request.getLocation().trim();
         if (normalized.isBlank()) {
            throw new IllegalArgumentException("Organization location must not be blank");
         }
         organization.setLocation(normalized);
      }
      if (request.getDescription() != null) {
         String normalized = request.getDescription().trim();
         if (normalized.isBlank()) {
            throw new IllegalArgumentException("Organization description must not be blank");
         }
         organization.setDescription(normalized);
      }
      if (request.getProfileImageFileId() != null) {
         StoredFile file = storedFileRepository.findById(request.getProfileImageFileId())
               .orElseThrow(() -> new EntityNotFoundException(
                     "File not found: " + request.getProfileImageFileId()));
         organization.setProfileImageFileId(file.getId());
      }

      Organization saved = organizationRepository.save(organization);

      if (request.getLinks() != null) {
         organizationLinkRepository.deleteByOrganizationId(organizationId);
         saveLinks(organizationId, request.getLinks());
      }

      return buildOrganizationDto(saved);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_EDIT')")
   public void deleteOrganizationProfileImage(Long organizationId) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      Long profileImageFileId = organization.getProfileImageFileId();
      if (profileImageFileId == null) {
         return;
      }

      organization.setProfileImageFileId(null);
      organizationRepository.save(organization);
      if (!fileReferenceService.isFileReferenced(profileImageFileId)) {
         fileStorageService.delete(profileImageFileId);
      }
   }

   /**
    * Получить организацию по идентификатору.
    */
   @Transactional(readOnly = true)
   public OrganizationDTO getOrganization(Long organizationId) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));
      return buildOrganizationDto(organization);
   }

   /**
    * Удалить организацию и связанные сущности верхнего уровня.
    *
    * На данный момент удаляются:
    * - внешние ссылки организации;
    * - пользователи организации;
    * - песни организации;
    * - роли с областью ORGANIZATION для этой организации.
    */
   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_DELETE')")
   public void deleteOrganization(Long organizationId) {
      if (!organizationRepository.existsById(organizationId)) {
         throw new EntityNotFoundException("Organization not found: " + organizationId);
      }
      deleteOrganizationCascade(organizationId);
   }


   @Transactional
   public void deleteOrganizationCascade(Long organizationId) {
      // 1. Delete events — DB cascade removes event_tags, event_participants,
      //    event_participant_songs, event_files, event_songs, event_sections, event_comment
      List<Event> events = eventRepository.findByOrganizationId(organizationId);
      if (!events.isEmpty()) {
         eventRepository.deleteAll(events);
      }

      // 2. Delete all tasks (both org-level and section-level) —
      //    DB cascade removes task_comment, task_files, task_visibility_role
      List<Task> tasks = taskRepository.findByOrganizationId(organizationId);
      if (!tasks.isEmpty()) {
         taskRepository.deleteAll(tasks);
      }

      // 3. Songs must come after events (event_songs has FK to song without cascade)
      var songsPage = songRepository.findByOrganizationId(
            organizationId,
            org.springframework.data.domain.Pageable.unpaged());
      List<Song> songs = songsPage.getContent();
      if (!songs.isEmpty()) {
         songRepository.deleteAll(songs); // DB cascade: song_files, song_instruments, song_tags
      }

      // 4. Sections: delete section_users and section roles first, then sections bottom-up
      List<Section> allSections = sectionRepository.findByOrganizationId(organizationId);
      if (!allSections.isEmpty()) {
         List<Long> allSectionIds = allSections.stream().map(Section::getId).toList();
         allSectionIds.forEach(sectionUserRepository::deleteBySectionId);

         List<Role> sectionRoles = roleRepository.findByScopeAndSectionIdIn(
               RoleScopeType.SECTION, allSectionIds);
         if (!sectionRoles.isEmpty()) {
            roleRepository.deleteAll(sectionRoles); // DB cascade: role_permission, user_role
         }

         sectionRepository.deleteAllById(getSectionIdsBottomUp(allSections));
      }

      // 5. Delete org-level roles — DB cascade removes role_permission and user_role
      List<Role> organizationRoles = roleRepository.findByScopeAndOrganizationId(
            RoleScopeType.ORGANIZATION,
            organizationId);
      if (!organizationRoles.isEmpty()) {
         roleRepository.deleteAll(organizationRoles);
      }

      // 6. Delete remaining org data
      organizationInviteRepository.deleteById(organizationId);
      organizationLinkRepository.deleteByOrganizationId(organizationId);
      organizationUserRepository.deleteByOrganizationId(organizationId);
      organizationRepository.deleteById(organizationId);
   }

   /**
    * Returns section IDs in bottom-up order (children before parents) for safe deletion.
    */
   private List<Long> getSectionIdsBottomUp(List<Section> sections) {
      Map<Long, List<Long>> childrenOf = new HashMap<>();
      Set<Long> allIds = new HashSet<>();
      for (Section s : sections) {
         allIds.add(s.getId());
         childrenOf.computeIfAbsent(s.getId(), k -> new ArrayList<>());
      }
      for (Section s : sections) {
         if (s.getParentSectionId() != null && allIds.contains(s.getParentSectionId())) {
            childrenOf.get(s.getParentSectionId()).add(s.getId());
         }
      }

      List<Long> result = new ArrayList<>();
      Set<Long> visited = new HashSet<>();
      for (Section s : sections) {
         if (s.getParentSectionId() == null || !allIds.contains(s.getParentSectionId())) {
            dfsPostOrder(s.getId(), childrenOf, visited, result);
         }
      }
      return result;
   }

   private void dfsPostOrder(Long id, Map<Long, List<Long>> childrenOf, Set<Long> visited, List<Long> result) {
      if (visited.contains(id)) return;
      visited.add(id);
      for (Long childId : childrenOf.getOrDefault(id, List.of())) {
         dfsPostOrder(childId, childrenOf, visited, result);
      }
      result.add(id);
   }


   /**
    * Явно перегенерировать пригласительную ссылку для организации.
    * Доступно только обладателям права ORG_MEMBER_INVITE в контексте организации.
    */
   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'ORG_MEMBER_INVITE')")
   public String regenerateInviteLink(Long organizationId) {
      organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      Long currentUserId = securityUtils.getCurrentUserId();
      OrganizationInvite invite = createOrRegenerateInvite(organizationId, currentUserId);
      return invite.getCode();
   }

   /**
    * Поиск организаций по названию.
    * Пустой или null запрос возвращает все организации.
    */
   @Transactional(readOnly = true)
   public List<OrganizationDTO> searchPublicOrganizationsByName(String nameQuery) {
      String normalized = nameQuery == null ? null : nameQuery.trim();

      List<Organization> organizations;
      if (normalized == null || normalized.isBlank()) {
         organizations = organizationRepository.findAll();
      } else {
         organizations = organizationRepository.findByNameContainingIgnoreCase(normalized);
      }

      return organizations.stream()
            .map(this::buildOrganizationDto)
            .collect(Collectors.toList());
   }

   @Transactional(readOnly = true)
   public Page<OrganizationDTO> searchPublicOrganizationsByName(
         String nameQuery, Pageable pageable) {
      String normalized = nameQuery == null ? null : nameQuery.trim();

      Page<Organization> organizations;
      if (normalized == null || normalized.isBlank()) {
         organizations = organizationRepository.findAll(pageable);
      } else {
         organizations = organizationRepository.findByNameContainingIgnoreCase(normalized, pageable);
      }

      return organizations.map(this::buildOrganizationDto);
   }

   private OrganizationDTO buildOrganizationDto(Organization organization) {
      OrganizationDTO dto = organizationMapper.toDto(organization);

      List<OrganizationLink> links = organizationLinkRepository.findByOrganizationId(organization.getId());
      List<OrganizationLinkDTO> linkDtos = links.stream()
            .map(link -> new OrganizationLinkDTO(link.getLinkType(), link.getUrl()))
            .toList();

      dto.setLinks(linkDtos);
      return dto;
   }

   private void saveLinks(Long organizationId, List<OrganizationLinkDTO> links) {
      if (links == null || links.isEmpty()) {
         return;
      }
      if (links.size() > fileLimitsProperties.getOrganizationMaxFiles()) {
         throw new IllegalArgumentException(
               "Organization cannot have more than " + fileLimitsProperties.getOrganizationMaxFiles() + " links");
      }

      Map<String, OrganizationLinkDTO> uniqueLinks = new LinkedHashMap<>();
      for (OrganizationLinkDTO dto : links) {
         if (dto == null || dto.getLinkType() == null || dto.getUrl() == null) {
            continue;
         }
          String key = dto.getLinkType().name() + "|" + dto.getUrl().trim().toLowerCase(Locale.ROOT);
         uniqueLinks.putIfAbsent(key, dto);
      }

      List<OrganizationLink> entities = uniqueLinks.values().stream()
            .map(dto -> OrganizationLink.builder()
                  .organizationId(organizationId)
                  .linkType(dto.getLinkType())
                  .url(dto.getUrl().trim())
                  .build())
            .toList();

      organizationLinkRepository.saveAll(entities);
   }

   /**
    * Сгенерировать или перегенерировать пригласительную ссылку для организации.
    * Хранится один актуальный код на организацию (PRIMARY KEY по organization_id).
    */
   private OrganizationInvite createOrRegenerateInvite(Long organizationId, Long createdByUserId) {
      String code = generateInviteCode();

      OrganizationInvite invite = organizationInviteRepository.findById(organizationId)
            .orElse(OrganizationInvite.builder().organizationId(organizationId).build());

      invite.setCode(code);
      invite.setCreatedByUserId(createdByUserId);
      invite.setCreatedAt(Instant.now());

      return organizationInviteRepository.save(invite);
   }

   private String generateInviteCode() {
      // 32 символа алфавита [0-9A-Za-z], ~190 бит энтропии
      final String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
      final int length = 32;

      java.security.SecureRandom random = new java.security.SecureRandom();
      StringBuilder sb = new StringBuilder(length);
      for (int i = 0; i < length; i++) {
         int idx = random.nextInt(alphabet.length());
         sb.append(alphabet.charAt(idx));
      }
      return sb.toString();
   }

   private void ensureOrganizationBaseRoles(Long organizationId) {
      List<Role> existing = roleRepository.findByScopeAndOrganizationId(
            RoleScopeType.ORGANIZATION,
            organizationId);

      boolean hasLeader = existing.stream().anyMatch(r -> "Leader".equals(r.getName()));
      boolean hasCoLeader = existing.stream().anyMatch(r -> "Co-leader".equals(r.getName()));

      if (hasLeader && hasCoLeader) {
         return;
      }

      List<Role> templates = roleRepository.findByScopeAndSystemTrue(RoleScopeType.ORGANIZATION);
      Map<String, Role> templateByName = new HashMap<>();
      for (Role template : templates) {
         templateByName.put(template.getName(), template);
      }

      List<Role> toCreate = new ArrayList<>();

      if (!hasLeader && templateByName.containsKey("Leader")) {
         Role template = templateByName.get("Leader");
         Role role = Role.builder()
               .scope(RoleScopeType.ORGANIZATION)
               .organizationId(organizationId)
               .name(template.getName())
               .system(true)
               .createdAt(Instant.now())
               .build();
         toCreate.add(role);
      }

      if (!hasCoLeader && templateByName.containsKey("Co-leader")) {
         Role template = templateByName.get("Co-leader");
         Role role = Role.builder()
               .scope(RoleScopeType.ORGANIZATION)
               .organizationId(organizationId)
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
    * Brings organization leadership roles to a consistent state relative to the current membership.
    * <p>
    * Invariants enforced:
    * - if there is at least 1 accepted member, the organization must have exactly one {@code Leader}
    * - if there are 2+ accepted members, the organization must have exactly one {@code Co-leader}
    * - if there is exactly 1 member, there must be no {@code Co-leader} assignment
    * <p>
    * First member becomes Leader, second member becomes Co-leader. Priority is to keep current
    * assignee if they are still a member.
    */
   public void syncOrganizationLeadershipRoles(Long organizationId) {
      if (!organizationRepository.existsById(organizationId)) {
         return;
      }

      List<OrganizationUser> members = organizationUserRepository
            .findByOrganizationIdAndStatusOrderByJoinedAtAsc(organizationId, OrganizationUserStatusType.ACCEPTED);
      if (members.isEmpty()) {
         return;
      }

      List<Long> memberUserIds = members.stream().map(OrganizationUser::getUserId).toList();

      Long leaderUserId = roleRepository.findByScopeAndOrganizationIdAndName(
                  RoleScopeType.ORGANIZATION, organizationId, "Leader")
            .map(leaderRole -> ensureSingleRoleAssignee(leaderRole.getId(), memberUserIds,
                  memberUserIds.getFirst(), null))
            .orElse(null);

      roleRepository.findByScopeAndOrganizationIdAndName(
                  RoleScopeType.ORGANIZATION, organizationId, "Co-leader")
            .ifPresent(coLeaderRole -> {
               if (memberUserIds.size() < 2) {
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
}
