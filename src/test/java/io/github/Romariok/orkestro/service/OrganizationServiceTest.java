package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dto.organization.OrganizationCreateRequestDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationLinkDTO;
import io.github.Romariok.orkestro.dto.organization.OrganizationUpdateRequestDTO;
import io.github.Romariok.orkestro.mapper.OrganizationMapper;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.enums.LinkType;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

   @Mock
   private OrganizationRepository organizationRepository;

   @Mock
   private OrganizationLinkRepository organizationLinkRepository;

   @Mock
   private OrganizationUserRepository organizationUserRepository;

   @Mock
   private StoredFileRepository storedFileRepository;

   @Mock
   private SongRepository songRepository;

   @Mock
   private RoleRepository roleRepository;

   @Mock
   private OrganizationMapper organizationMapper;

   @InjectMocks
   private OrganizationService organizationService;

   @Test
   void createOrganization_success_savesOrganizationAndLinks() {
      OrganizationCreateRequestDTO request = new OrganizationCreateRequestDTO(
            "Orkestro",
            "Moscow",
            "Wind orchestra",
            10L,
            VisibilityLevelType.PUBLIC,
            List.of(
                  new OrganizationLinkDTO(LinkType.WEBSITE, "https://orkestro.example"),
                  new OrganizationLinkDTO(LinkType.YOUTUBE, "https://youtube.com/orkestro")));

      StoredFile file = StoredFile.builder()
            .id(10L)
            .name("profile.png")
            .build();
      when(storedFileRepository.findById(10L)).thenReturn(Optional.of(file));

      Organization saved = new Organization();
      saved.setId(1L);
      saved.setName("Orkestro");
      saved.setLocation("Moscow");
      saved.setProfileImageFileId(10L);
      saved.setCreatedAt(Instant.now());
      saved.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.save(any(Organization.class))).thenReturn(saved);

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      baseDto.setName("Orkestro");
      when(organizationMapper.toDto(saved)).thenReturn(baseDto);

      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of(
            buildLinkEntity(1L, LinkType.WEBSITE, "https://orkestro.example"),
            buildLinkEntity(1L, LinkType.YOUTUBE, "https://youtube.com/orkestro")));

      OrganizationDTO result = organizationService.createOrganization(request);

      ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
      verify(organizationRepository).save(orgCaptor.capture());
      Organization persisted = orgCaptor.getValue();

      assertEquals("Orkestro", persisted.getName());
      assertEquals("Moscow", persisted.getLocation());
      assertEquals(10L, persisted.getProfileImageFileId());
      assertEquals(VisibilityLevelType.PUBLIC, persisted.getVisibilityLevel());

      verify(organizationLinkRepository).saveAll(any());

      assertEquals(1L, result.getId());
      assertEquals(2, result.getLinks().size());
      assertEquals(LinkType.WEBSITE, result.getLinks().getFirst().getLinkType());
      assertEquals("https://orkestro.example", result.getLinks().getFirst().getUrl());
   }

   @Test
   void createOrganization_profileImageFileNotFound_throwsEntityNotFound() {
      OrganizationCreateRequestDTO request = new OrganizationCreateRequestDTO(
            "Orkestro",
            "Moscow",
            null,
            999L,
            VisibilityLevelType.PUBLIC,
            null);

      when(storedFileRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.createOrganization(request));

      verify(organizationRepository, never()).save(any());
      verify(organizationLinkRepository, never()).saveAll(any());
   }

   @Test
   void updateOrganization_notFound_throwsEntityNotFound() {
      when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.updateOrganization(1L, new OrganizationUpdateRequestDTO()));

      verify(organizationRepository, never()).save(any());
   }

   @Test
   void updateOrganization_updatesBasicFieldsAndProfileImage() {
      Organization existing = new Organization();
      existing.setId(1L);
      existing.setName("Old name");
      existing.setLocation("Old location");
      existing.setDescription("Old description");
      existing.setProfileImageFileId(10L);
      existing.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

      StoredFile newFile = StoredFile.builder()
            .id(20L)
            .name("new.png")
            .build();
      when(storedFileRepository.findById(20L)).thenReturn(Optional.of(newFile));

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      when(organizationMapper.toDto(existing)).thenReturn(baseDto);
      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of());

      OrganizationUpdateRequestDTO request = new OrganizationUpdateRequestDTO();
      request.setName("New name");
      request.setLocation("New location");
      request.setDescription("New description");
      request.setProfileImageFileId(20L);

      organizationService.updateOrganization(1L, request);

      assertEquals("New name", existing.getName());
      assertEquals("New location", existing.getLocation());
      assertEquals("New description", existing.getDescription());
      assertEquals(20L, existing.getProfileImageFileId());

      verify(organizationLinkRepository, never()).deleteByOrganizationId(1L);
   }

   @Test
   void updateOrganization_profileImageFileNotFound_throwsEntityNotFound() {
      Organization existing = new Organization();
      existing.setId(1L);
      existing.setProfileImageFileId(10L);

      when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(storedFileRepository.findById(999L)).thenReturn(Optional.empty());

      OrganizationUpdateRequestDTO request = new OrganizationUpdateRequestDTO();
      request.setProfileImageFileId(999L);

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.updateOrganization(1L, request));

      verify(organizationRepository, never()).save(any());
      verify(organizationLinkRepository, never()).deleteByOrganizationId(any());
   }

   @Test
   void updateOrganization_withLinks_replacesLinks() {
      Organization existing = new Organization();
      existing.setId(1L);
      existing.setName("Orkestro");
      existing.setLocation("Moscow");
      existing.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(organizationRepository.save(any(Organization.class))).thenReturn(existing);

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      when(organizationMapper.toDto(existing)).thenReturn(baseDto);
      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of());

      OrganizationUpdateRequestDTO request = new OrganizationUpdateRequestDTO();
      request.setLinks(List.of(
            new OrganizationLinkDTO(LinkType.WEBSITE, "https://new.example")));

      organizationService.updateOrganization(1L, request);

      verify(organizationLinkRepository).deleteByOrganizationId(1L);
      verify(organizationLinkRepository).saveAll(any());
   }

   @Test
   void getOrganization_notFound_throwsEntityNotFound() {
      when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.getOrganization(1L));
   }

   @Test
   void getOrganization_returnsDtoWithLinks() {
      Organization organization = new Organization();
      organization.setId(1L);
      organization.setName("Orkestro");
      organization.setLocation("Moscow");
      organization.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      baseDto.setName("Orkestro");
      when(organizationMapper.toDto(organization)).thenReturn(baseDto);

      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of(
            buildLinkEntity(1L, LinkType.WEBSITE, "https://orkestro.example")));

      OrganizationDTO dto = organizationService.getOrganization(1L);

      assertEquals(1L, dto.getId());
      assertEquals(1, dto.getLinks().size());
      assertEquals(LinkType.WEBSITE, dto.getLinks().getFirst().getLinkType());
   }

   @Test
   void deleteOrganization_notFound_throwsEntityNotFound() {
      when(organizationRepository.existsById(1L)).thenReturn(false);

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.deleteOrganization(1L));

      verify(organizationLinkRepository, never()).deleteByOrganizationId(any());
      verify(organizationUserRepository, never()).deleteByOrganizationId(any());
      verify(songRepository, never()).findByOrganizationId(any(), any(Pageable.class));
      verify(organizationRepository, never()).deleteById(any());
   }

   @Test
   void deleteOrganization_existing_cleansDependenciesAndDeletesOrganization() {
      when(organizationRepository.existsById(1L)).thenReturn(true);

      Song song = Song.builder()
            .id(100L)
            .organizationId(1L)
            .title("Song")
            .createdAt(Instant.now())
            .build();
      Page<Song> songPage = new PageImpl<>(List.of(song));
      when(songRepository.findByOrganizationId(1L, Pageable.unpaged())).thenReturn(songPage);

      Role role = Role.builder()
            .id(10L)
            .build();
      when(roleRepository.findByScopeAndOrganizationId(RoleScopeType.ORGANIZATION, 1L))
            .thenReturn(List.of(role));

      organizationService.deleteOrganization(1L);

      verify(organizationLinkRepository).deleteByOrganizationId(1L);
      verify(organizationUserRepository).deleteByOrganizationId(1L);
      verify(songRepository).findByOrganizationId(1L, Pageable.unpaged());
      verify(songRepository).deleteAll(List.of(song));
      verify(roleRepository).deleteAll(List.of(role));
      verify(organizationRepository).deleteById(1L);
   }

   @Test
   void setVisibility_notFound_throwsEntityNotFound() {
      when(organizationRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> organizationService.setVisibility(1L, VisibilityLevelType.PRIVATE));
   }

   @Test
   void setVisibility_updatesVisibilityLevel() {
      Organization organization = new Organization();
      organization.setId(1L);
      organization.setName("Orkestro");
      organization.setLocation("Moscow");
      organization.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
      when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      baseDto.setVisibilityLevel(VisibilityLevelType.PRIVATE);
      when(organizationMapper.toDto(organization)).thenReturn(baseDto);
      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of());

      OrganizationDTO result = organizationService.setVisibility(1L, VisibilityLevelType.PRIVATE);

      assertEquals(VisibilityLevelType.PRIVATE, organization.getVisibilityLevel());
      assertEquals(VisibilityLevelType.PRIVATE, result.getVisibilityLevel());
   }

   @Test
   void searchPublicOrganizationsByName_blankQuery_returnsAllPublic() {
      Organization org = new Organization();
      org.setId(1L);
      org.setName("Orkestro");
      org.setLocation("Moscow");
      org.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findByVisibilityLevel(VisibilityLevelType.PUBLIC))
            .thenReturn(List.of(org));

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      baseDto.setName("Orkestro");
      when(organizationMapper.toDto(org)).thenReturn(baseDto);
      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of());

      List<OrganizationDTO> result = organizationService.searchPublicOrganizationsByName("   ");

      assertEquals(1, result.size());
      assertEquals(1L, result.getFirst().getId());
   }

   @Test
   void searchPublicOrganizationsByName_withName_filtersByNameAndVisibility() {
      Organization org = new Organization();
      org.setId(1L);
      org.setName("Orkestro Band");
      org.setLocation("Moscow");
      org.setVisibilityLevel(VisibilityLevelType.PUBLIC);

      when(organizationRepository.findByVisibilityLevelAndNameContainingIgnoreCase(
            VisibilityLevelType.PUBLIC,
            "Ork"))
            .thenReturn(List.of(org));

      OrganizationDTO baseDto = new OrganizationDTO();
      baseDto.setId(1L);
      baseDto.setName("Orkestro Band");
      when(organizationMapper.toDto(org)).thenReturn(baseDto);
      when(organizationLinkRepository.findByOrganizationId(1L)).thenReturn(List.of());

      List<OrganizationDTO> result = organizationService.searchPublicOrganizationsByName(" Ork ");

      assertEquals(1, result.size());
      assertEquals("Orkestro Band", result.getFirst().getName());
      verify(organizationRepository).findByVisibilityLevelAndNameContainingIgnoreCase(
            VisibilityLevelType.PUBLIC,
            "Ork");
   }

   private OrganizationLink buildLinkEntity(Long organizationId, LinkType linkType, String url) {
      OrganizationLink link = new OrganizationLink();
      link.setOrganizationId(organizationId);
      link.setLinkType(linkType);
      link.setUrl(url);
      return link;
   }
}
