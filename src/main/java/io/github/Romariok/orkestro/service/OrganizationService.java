package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dto.organization.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationLinkDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.mapper.OrganizationMapper;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.enums.VisibilityLevelType;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.organization.OrganizationLink;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.song.Song;
import io.github.Romariok.orkestro.repository.OrganizationLinkRepository;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SongRepository;
import io.github.Romariok.orkestro.repository.StoredFileRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

   private final OrganizationRepository organizationRepository;
   private final OrganizationLinkRepository organizationLinkRepository;
   private final OrganizationUserRepository organizationUserRepository;
   private final StoredFileRepository storedFileRepository;
   private final SongRepository songRepository;
   private final RoleRepository roleRepository;
   private final OrganizationMapper organizationMapper;

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

      return buildOrganizationDto(saved);
   }

   /**
    * Обновить параметры организации (кроме уровня видимости).
    */
   @Transactional
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
   public void deleteOrganization(Long organizationId) {
      if (!organizationRepository.existsById(organizationId)) {
         throw new EntityNotFoundException("Organization not found: " + organizationId);
      }

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
   public OrganizationDTO setVisibility(Long organizationId, VisibilityLevelType visibilityLevel) {
      Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

      organization.setVisibilityLevel(visibilityLevel);
      Organization saved = organizationRepository.save(organization);

      return buildOrganizationDto(saved);
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
}
