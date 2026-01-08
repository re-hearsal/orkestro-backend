package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dto.organization.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationLinkDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.mapper.OrganizationMapper;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.organization.OrganizationLink;
import io.github.Romariok.orkestro.models.organization.OrganizationInvite;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.role.Permission;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.role.RolePermission;
import io.github.Romariok.orkestro.models.song.Song;
import io.github.Romariok.orkestro.repository.OrganizationInviteRepository;
import io.github.Romariok.orkestro.repository.OrganizationLinkRepository;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SongRepository;
import io.github.Romariok.orkestro.repository.StoredFileRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
   private final StoredFileRepository storedFileRepository;
   private final SongRepository songRepository;
   private final RoleRepository roleRepository;
   private final OrganizationMapper organizationMapper;
   private final RolePermissionRepository rolePermissionRepository;
   private final SecurityUtils securityUtils;
   private final TechnicalRoleService technicalRoleService;

   @Transactional
   public OrganizationDTO createOrganization(OrganizationCreateRequestDTO request) {
      // Проверяем, что файл профиля существует
      StoredFile file = storedFileRepository.findById(request.getProfileImageFileId())
            .orElseThrow(() -> new EntityNotFoundException(
                  "File not found: " + request.getProfileImageFileId()));

      Organization organization = Organization.builder()
            .name(request.getName())
            .location(request.getLocation())
            .description(request.getDescription())
            .profileImageFileId(file.getId())
            .createdAt(Instant.now())
            .visibilityLevel(request.getVisibilityLevel())
            .build();

      Organization saved = organizationRepository.save(organization);

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

      // и получает роль Leader внутри этой организации
      ensureOrganizationBaseRoles(saved.getId());

      Role leaderRole = roleRepository.findByScopeAndOrganizationIdAndName(
            RoleScopeType.ORGANIZATION,
            saved.getId(),
            "Leader")
            .orElseThrow(() -> new EntityNotFoundException(
                  "Role not found for organization " + saved.getId() + " and name Leader"));

      technicalRoleService.assignOrganizationRoleToUser(saved.getId(), creatorId, leaderRole.getId());

      // если организация создаётся сразу приватной — генерируем пригласительную
      // ссылку
      if (saved.getVisibilityLevel() == VisibilityLevelType.PRIVATE) {
         createOrRegenerateInvite(saved.getId(), creatorId);
      }

      return buildOrganizationDto(saved);
   }

   /**
    * Обновить параметры организации (кроме уровня видимости).
    */
   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_EDIT')")
   public OrganizationDTO updateOrganization(Long organizationId, OrganizationUpdateRequestDTO request) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      if (request.getName() != null) {
         organization.setName(request.getName());
      }
      if (request.getLocation() != null) {
         organization.setLocation(request.getLocation());
      }
      if (request.getDescription() != null) {
         organization.setDescription(request.getDescription());
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
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_DELETE')")
   public void deleteOrganization(Long organizationId) {
      if (!organizationRepository.existsById(organizationId)) {
         throw new EntityNotFoundException("Organization not found: " + organizationId);
      }

      // Удаляем возможную пригласительную ссылку, чтобы не нарушить внешние ключи
      organizationInviteRepository.deleteById(organizationId);

      // Очистка зависимостей, которые уже представлены репозиториями в проекте
      organizationLinkRepository.deleteByOrganizationId(organizationId);
      organizationUserRepository.deleteByOrganizationId(organizationId);

      var songsPage = songRepository.findByOrganizationId(
            organizationId,
            org.springframework.data.domain.Pageable.unpaged());
      List<Song> songs = songsPage.getContent();
      if (!songs.isEmpty()) {
         songRepository.deleteAll(songs);
      }

      List<Role> organizationRoles = roleRepository.findByScopeAndOrganizationId(
            RoleScopeType.ORGANIZATION,
            organizationId);
      if (!organizationRoles.isEmpty()) {
         roleRepository.deleteAll(organizationRoles);
      }

      organizationRepository.deleteById(organizationId);
   }

   /**
    * Установить уровень видимости организации.
    */
   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_SET_VISIBILITY')")
   public OrganizationDTO setVisibility(Long organizationId, VisibilityLevelType visibilityLevel) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      VisibilityLevelType previousLevel = organization.getVisibilityLevel();
      organization.setVisibilityLevel(visibilityLevel);
      Organization saved = organizationRepository.save(organization);

      if (visibilityLevel == VisibilityLevelType.PRIVATE && previousLevel == VisibilityLevelType.PUBLIC) {
         Long currentUserId = securityUtils.getCurrentUserId();
         createOrRegenerateInvite(organizationId, currentUserId);
      }

      if (visibilityLevel == VisibilityLevelType.PUBLIC && previousLevel == VisibilityLevelType.PRIVATE) {
         organizationInviteRepository.deleteById(organizationId);
      }

      return buildOrganizationDto(saved);
   }

   /**
    * Явно перегенерировать пригласительную ссылку для приватной организации.
    * Доступно только обладателям права ORG_MEMBER_INVITE в контексте организации.
    */
   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':ORG_MEMBER_INVITE')")
   public String regenerateInviteLink(Long organizationId) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      if (organization.getVisibilityLevel() != VisibilityLevelType.PRIVATE) {
         throw new io.github.Romariok.orkestro.utils.exception.BusinessException(
               "Invite links are available only for PRIVATE organizations");
      }

      Long currentUserId = securityUtils.getCurrentUserId();
      OrganizationInvite invite = createOrRegenerateInvite(organizationId, currentUserId);
      return invite.getCode();
   }

   /**
    * Поиск публичных организаций по названию.
    * Пустой или null запрос возвращает все публичные организации.
    */
   @Transactional(readOnly = true)
   public List<OrganizationDTO> searchPublicOrganizationsByName(String nameQuery) {
      String normalized = nameQuery == null ? null : nameQuery.trim();

      List<Organization> organizations;
      if (normalized == null || normalized.isBlank()) {
         organizations = organizationRepository.findByVisibilityLevel(VisibilityLevelType.PUBLIC);
      } else {
         organizations = organizationRepository.findByVisibilityLevelAndNameContainingIgnoreCase(
               VisibilityLevelType.PUBLIC,
               normalized);
      }

      return organizations.stream()
            .map(this::buildOrganizationDto)
            .collect(Collectors.toList());
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

      List<OrganizationLink> entities = links.stream()
            .map(dto -> OrganizationLink.builder()
                  .organizationId(organizationId)
                  .linkType(dto.getLinkType())
                  .url(dto.getUrl())
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
}
