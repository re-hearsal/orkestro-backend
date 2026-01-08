package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dto.section.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.dto.section.SectionDTO;
import io.github.Romariok.orkestro.mapper.SectionMapper;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.section.Section;
import io.github.Romariok.orkestro.models.task.Task;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SectionRepository;
import io.github.Romariok.orkestro.repository.SectionUserRepository;
import io.github.Romariok.orkestro.repository.TaskRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

        @Mock
        private SectionRepository sectionRepository;

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private SectionUserRepository sectionUserRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private TaskRepository taskRepository;

        @Mock
        private SectionMapper sectionMapper;

        @InjectMocks
        private SectionService sectionService;

        @Test
        void createSectionInOrganization_success_savesSection() {
                Long organizationId = 1L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Brass", "Brass section");

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orkestro")
                                .location("Moscow")
                                .profileImageFileId(10L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(sectionRepository.existsByOrganizationIdAndParentSectionIdIsNullAndName(organizationId, "Brass"))
                                .thenReturn(false);

                Section saved = new Section();
                saved.setId(10L);
                saved.setName("Brass");
                saved.setDescription("Brass section");
                saved.setOrganizationId(organizationId);

                when(sectionRepository.save(any(Section.class))).thenReturn(saved);

                SectionDTO dto = new SectionDTO(10L, "Brass", "Brass section", organizationId, null);
                when(sectionMapper.toDto(saved)).thenReturn(dto);

                SectionDTO result = sectionService.createSectionInOrganization(organizationId, request);

                ArgumentCaptor<Section> sectionCaptor = ArgumentCaptor.forClass(Section.class);
                verify(sectionRepository).save(sectionCaptor.capture());
                Section persisted = sectionCaptor.getValue();

                assertEquals("Brass", persisted.getName());
                assertEquals("Brass section", persisted.getDescription());
                assertEquals(organizationId, persisted.getOrganizationId());
                assertEquals(10L, result.getId());
                assertEquals("Brass", result.getName());
        }

        @Test
        void createSectionInOrganization_organizationNotFound_throwsEntityNotFound() {
                Long organizationId = 1L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Brass", "Brass section");

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.createSectionInOrganization(organizationId, request));

                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInOrganization_duplicateName_throwsBusinessException() {
                Long organizationId = 1L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Brass", "Brass section");

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orkestro")
                                .location("Moscow")
                                .profileImageFileId(10L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(sectionRepository.existsByOrganizationIdAndParentSectionIdIsNullAndName(organizationId, "Brass"))
                                .thenReturn(true);

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.createSectionInOrganization(organizationId, request));

                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInOrganization_blankName_throwsIllegalArgumentException() {
                Long organizationId = 1L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("   ", "Brass section");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> sectionService.createSectionInOrganization(organizationId, request));

                verify(organizationRepository, never()).findById(any());
                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInSection_success_savesSection() {
                Long parentSectionId = 5L;
                Long organizationId = 1L;

                Section parent = new Section();
                parent.setId(parentSectionId);
                parent.setOrganizationId(organizationId);
                parent.setName("Brass");

                when(sectionRepository.findById(parentSectionId)).thenReturn(Optional.of(parent));
                when(sectionRepository.existsByOrganizationIdAndParentSectionIdAndName(organizationId, parentSectionId,
                                "Trumpets"))
                                .thenReturn(false);

                Section saved = new Section();
                saved.setId(20L);
                saved.setName("Trumpets");
                saved.setDescription("Trumpet group");
                saved.setOrganizationId(organizationId);
                saved.setParentSectionId(parentSectionId);

                when(sectionRepository.save(any(Section.class))).thenReturn(saved);

                SectionDTO dto = new SectionDTO(20L, "Trumpets", "Trumpet group", organizationId, parentSectionId);
                when(sectionMapper.toDto(saved)).thenReturn(dto);

                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Trumpets", "Trumpet group");

                SectionDTO result = sectionService.createSectionInSection(parentSectionId, request);

                ArgumentCaptor<Section> sectionCaptor = ArgumentCaptor.forClass(Section.class);
                verify(sectionRepository).save(sectionCaptor.capture());
                Section persisted = sectionCaptor.getValue();

                assertEquals("Trumpets", persisted.getName());
                assertEquals("Trumpet group", persisted.getDescription());
                assertEquals(organizationId, persisted.getOrganizationId());
                assertEquals(parentSectionId, persisted.getParentSectionId());
                assertEquals(20L, result.getId());
                assertEquals(parentSectionId, result.getParentSectionId());
        }

        @Test
        void createSectionInSection_parentNotFound_throwsEntityNotFound() {
                Long parentSectionId = 5L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Trumpets", "Trumpet group");

                when(sectionRepository.findById(parentSectionId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.createSectionInSection(parentSectionId, request));

                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInSection_duplicateName_throwsBusinessException() {
                Long parentSectionId = 5L;
                Long organizationId = 1L;

                Section parent = new Section();
                parent.setId(parentSectionId);
                parent.setOrganizationId(organizationId);

                when(sectionRepository.findById(parentSectionId)).thenReturn(Optional.of(parent));
                when(sectionRepository.existsByOrganizationIdAndParentSectionIdAndName(organizationId, parentSectionId,
                                "Trumpets"))
                                .thenReturn(true);

                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Trumpets", "Trumpet group");

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.createSectionInSection(parentSectionId, request));

                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInSection_blankName_throwsIllegalArgumentException() {
                Long parentSectionId = 5L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("   ", "Trumpet group");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> sectionService.createSectionInSection(parentSectionId, request));

                verify(sectionRepository, never()).findById(any());
                verify(sectionRepository, never()).save(any());
        }

        @Test
        void deleteSection_notFound_throwsEntityNotFound() {
                Long sectionId = 10L;
                when(sectionRepository.existsById(sectionId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.deleteSection(sectionId));

                verify(sectionUserRepository, never()).deleteBySectionId(any());
                verify(sectionRepository, never()).deleteAllById(any());
        }

        @Test
        void deleteSection_singleSection_cleansDependenciesAndDeletesSection() {
                Long sectionId = 10L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(sectionRepository.findByParentSectionId(sectionId)).thenReturn(List.of());

                when(taskRepository.findBySectionId(sectionId)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                sectionService.deleteSection(sectionId);

                verify(sectionUserRepository).deleteBySectionId(sectionId);
                verify(taskRepository).findBySectionId(sectionId);
                verify(roleRepository).findByScopeAndSectionId(RoleScopeType.SECTION, sectionId);
                verify(sectionRepository).deleteAllById(List.of(sectionId));
        }

        @Test
        void deleteSection_withChildSections_deletesSubtreeAndDependencies() {
                Long rootId = 1L;
                Long childId = 2L;

                when(sectionRepository.existsById(rootId)).thenReturn(true);

                Section child = new Section();
                child.setId(childId);
                child.setOrganizationId(100L);
                child.setParentSectionId(rootId);

                when(sectionRepository.findByParentSectionId(rootId)).thenReturn(List.of(child));
                when(sectionRepository.findByParentSectionId(childId)).thenReturn(List.of());

                Task childTask = new Task();
                childTask.setId(50L);
                childTask.setSectionId(childId);

                when(taskRepository.findBySectionId(childId)).thenReturn(List.of(childTask));
                when(taskRepository.findBySectionId(rootId)).thenReturn(List.of());

                Role childRole = Role.builder()
                                .id(200L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(childId)
                                .build();
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, childId))
                                .thenReturn(List.of(childRole));
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, rootId))
                                .thenReturn(List.of());

                sectionService.deleteSection(rootId);

                verify(sectionUserRepository).deleteBySectionId(childId);
                verify(sectionUserRepository).deleteBySectionId(rootId);

                verify(taskRepository).findBySectionId(childId);
                verify(taskRepository).findBySectionId(rootId);
                verify(taskRepository).delete(childTask);

                verify(roleRepository).findByScopeAndSectionId(RoleScopeType.SECTION, childId);
                verify(roleRepository).findByScopeAndSectionId(RoleScopeType.SECTION, rootId);
                verify(roleRepository).deleteAll(List.of(childRole));
                verify(sectionRepository).deleteAllById(List.of(childId, rootId));
        }
}
