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
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.section.Section;
import io.github.Romariok.orkestro.models.section.SectionUser;
import io.github.Romariok.orkestro.models.task.Task;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RolePermissionRepository;
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
        private OrganizationUserRepository organizationUserRepository;

        @Mock
        private SectionUserRepository sectionUserRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private RolePermissionRepository rolePermissionRepository;

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
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, 10L))
                                .thenReturn(List.of());
                when(roleRepository.findByScopeAndSystemTrue(RoleScopeType.SECTION)).thenReturn(List.of());

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
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, 20L))
                                .thenReturn(List.of());
                when(roleRepository.findByScopeAndSystemTrue(RoleScopeType.SECTION)).thenReturn(List.of());

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

        @Test
        void addUserToSection_rootSection_success_addsMembership() {
                Long sectionId = 10L;
                Long organizationId = 1L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(organizationId);
                section.setParentSectionId(null);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)).thenReturn(Optional.empty());

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                sectionService.addUserToSection(sectionId, userId);

                ArgumentCaptor<SectionUser> captor = ArgumentCaptor.forClass(SectionUser.class);
                verify(sectionUserRepository).save(captor.capture());
                SectionUser saved = captor.getValue();

                assertEquals(sectionId, saved.getSectionId());
                assertEquals(userId, saved.getUserId());
        }

        @Test
        void addUserToSection_rootSection_userNotInOrganization_throwsBusinessException() {
                Long sectionId = 10L;
                Long organizationId = 1L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(organizationId);
                section.setParentSectionId(null);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)).thenReturn(Optional.empty());
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.addUserToSection(sectionId, userId));

                verify(sectionUserRepository, never()).save(any());
        }

        @Test
        void addUserToSection_rootSection_userNotAccepted_throwsBusinessException() {
                Long sectionId = 10L;
                Long organizationId = 1L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(organizationId);
                section.setParentSectionId(null);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)).thenReturn(Optional.empty());

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.PENDING)
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.addUserToSection(sectionId, userId));

                verify(sectionUserRepository, never()).save(any());
        }

        @Test
        void addUserToSection_childSection_success_addsMembership() {
                Long rootId = 1L;
                Long sectionId = 2L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(10L);
                section.setParentSectionId(rootId);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)).thenReturn(Optional.empty());

                SectionUser parentMembership = new SectionUser();
                parentMembership.setSectionId(rootId);
                parentMembership.setUserId(userId);
                when(sectionUserRepository.findBySectionIdAndUserId(rootId, userId))
                                .thenReturn(Optional.of(parentMembership));

                sectionService.addUserToSection(sectionId, userId);

                ArgumentCaptor<SectionUser> captor = ArgumentCaptor.forClass(SectionUser.class);
                verify(sectionUserRepository).save(captor.capture());
                SectionUser saved = captor.getValue();

                assertEquals(sectionId, saved.getSectionId());
                assertEquals(userId, saved.getUserId());
        }

        @Test
        void addUserToSection_childSection_userNotInParent_throwsBusinessException() {
                Long rootId = 1L;
                Long sectionId = 2L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(10L);
                section.setParentSectionId(rootId);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId)).thenReturn(Optional.empty());
                when(sectionUserRepository.findBySectionIdAndUserId(rootId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.addUserToSection(sectionId, userId));

                verify(sectionUserRepository, never()).save(any());
        }

        @Test
        void addUserToSection_sectionNotFound_throwsEntityNotFound() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.addUserToSection(sectionId, userId));

                verify(sectionUserRepository, never()).save(any());
        }

        @Test
        void addUserToSection_alreadyMember_doesNothing() {
                Long sectionId = 10L;
                Long userId = 100L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(1L);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

                SectionUser existing = new SectionUser();
                existing.setSectionId(sectionId);
                existing.setUserId(userId);
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.of(existing));

                sectionService.addUserToSection(sectionId, userId);

                verify(sectionUserRepository, never()).save(any());
                verify(organizationUserRepository, never()).findByOrganizationIdAndUserId(any(), any());
        }

        @Test
        void removeUserFromSection_sectionNotFound_throwsEntityNotFound() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.removeUserFromSection(sectionId, userId));

                verify(sectionUserRepository, never()).findBySectionIdAndUserId(any(), any());
        }

        @Test
        void removeUserFromSection_existingMembership_deletesIt() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);

                SectionUser su = new SectionUser();
                su.setSectionId(sectionId);
                su.setUserId(userId);

                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.of(su));

                sectionService.removeUserFromSection(sectionId, userId);

                verify(sectionUserRepository).delete(su);
        }

        @Test
        void removeUserFromSection_noMembership_doesNothing() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.empty());

                sectionService.removeUserFromSection(sectionId, userId);

                verify(sectionUserRepository, never()).delete(any());
        }
}
