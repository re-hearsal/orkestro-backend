package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.section.dto.SectionCreateRequestDTO;
import io.github.Romariok.orkestro.section.dto.SectionDTO;
import io.github.Romariok.orkestro.section.mapper.SectionMemberMapper;
import io.github.Romariok.orkestro.section.mapper.SectionMapper;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.section.service.SectionService;
import io.github.Romariok.orkestro.event.repository.EventSectionRepository;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.exception.SectionDepthExceededException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        private EventSectionRepository eventSectionRepository;

        @Mock
        private SectionMapper sectionMapper;

        @Mock
        private SectionMemberMapper sectionMemberMapper;

        @Mock
        private UserRoleRepository userRoleRepository;

        @Mock
        private SecurityUtils securityUtils;

        @InjectMocks
        private SectionService sectionService;

        @Test
        void createSectionInOrganization_success_savesSection() {
                Long organizationId = 1L;
                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Brass", "Brass section");
                Long currentUserId = 100L;

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
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(sectionUserRepository.findBySectionIdAndUserId(10L, currentUserId)).thenReturn(Optional.empty());
                when(sectionRepository.existsById(10L)).thenReturn(true);
                SectionUser creatorMembership = new SectionUser();
                creatorMembership.setSectionId(10L);
                creatorMembership.setUserId(currentUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(10L))
                                .thenReturn(List.of(creatorMembership));
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 10L, "Leader"))
                                .thenReturn(Optional.empty());
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 10L, "Co-leader"))
                                .thenReturn(Optional.empty());

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
                Long currentUserId = 100L;

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
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(sectionUserRepository.findBySectionIdAndUserId(20L, currentUserId)).thenReturn(Optional.empty());
                when(sectionRepository.existsById(20L)).thenReturn(true);
                SectionUser creatorMembership = new SectionUser();
                creatorMembership.setSectionId(20L);
                creatorMembership.setUserId(currentUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(20L))
                                .thenReturn(List.of(creatorMembership));
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 20L, "Leader"))
                                .thenReturn(Optional.empty());
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 20L, "Co-leader"))
                                .thenReturn(Optional.empty());

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

                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                sectionService.deleteSection(sectionId);

                verify(sectionUserRepository).deleteBySectionId(sectionId);
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
        void removeUserFromSection_cannotRemoveSelf_throwsIllegalArgumentException() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(userId);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> sectionService.removeUserFromSection(sectionId, userId));

                verify(sectionUserRepository, never()).findBySectionIdAndUserId(any(), any());
                verify(sectionUserRepository, never()).deleteBySectionIdInAndUserId(any(), any());
        }

        @Test
        void removeUserFromSection_existingMembership_lastMember_deletesSection() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(sectionRepository.findByParentSectionId(sectionId)).thenReturn(List.of());

                SectionUser su = new SectionUser();
                su.setSectionId(sectionId);
                su.setUserId(userId);

                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.of(su));
                when(roleRepository.findByScopeAndSectionIdIn(eq(RoleScopeType.SECTION), any()))
                                .thenReturn(List.of());
                when(sectionUserRepository.countBySectionId(sectionId)).thenReturn(0L);
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                sectionService.removeUserFromSection(sectionId, userId);

                verify(sectionUserRepository).deleteBySectionIdInAndUserId(eq(List.of(sectionId)), eq(userId));
                verify(sectionRepository).deleteAllById(List.of(sectionId));
        }

        @Test
        void removeUserFromSection_noMembership_doesNothing() {
                Long sectionId = 10L;
                Long userId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.empty());

                sectionService.removeUserFromSection(sectionId, userId);

                verify(sectionUserRepository, never()).deleteBySectionIdInAndUserId(any(), any());
        }

        @Test
        void leaveCurrentSection_sectionNotFound_throwsEntityNotFound() {
                Long sectionId = 10L;
                when(sectionRepository.existsById(sectionId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> sectionService.leaveCurrentSection(sectionId));

                verify(sectionUserRepository, never()).deleteBySectionIdInAndUserId(any(), any());
        }

        @Test
        @SuppressWarnings("unchecked")
        void leaveCurrentSection_deletesMembershipInSubtree() {
                Long rootId = 1L;
                Long childId = 2L;
                Long currentUserId = 100L;

                when(sectionRepository.existsById(rootId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                SectionUser member = new SectionUser();
                member.setSectionId(rootId);
                member.setUserId(currentUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(rootId)).thenReturn(List.of(member));

                Section child = new Section();
                child.setId(childId);
                child.setParentSectionId(rootId);
                when(sectionRepository.findByParentSectionId(rootId)).thenReturn(List.of(child));
                when(sectionRepository.findByParentSectionId(childId)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSectionIdIn(eq(RoleScopeType.SECTION), any()))
                                .thenReturn(List.of());
                when(sectionUserRepository.countBySectionId(rootId)).thenReturn(1L);

                sectionService.leaveCurrentSection(rootId);

                ArgumentCaptor<Collection<Long>> sectionIdsCaptor = ArgumentCaptor.forClass(Collection.class);
                verify(sectionUserRepository).deleteBySectionIdInAndUserId(sectionIdsCaptor.capture(),
                                eq(currentUserId));

                Set<Long> ids = new HashSet<>(sectionIdsCaptor.getValue());
                assertEquals(Set.of(childId, rootId), ids);
        }

        @Test
        void leaveCurrentSection_lastMember_deletesSection() {
                Long sectionId = 10L;
                Long currentUserId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                SectionUser member = new SectionUser();
                member.setSectionId(sectionId);
                member.setUserId(currentUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId)).thenReturn(List.of(member));
                when(sectionRepository.findByParentSectionId(sectionId)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSectionIdIn(eq(RoleScopeType.SECTION), any()))
                                .thenReturn(List.of());
                when(sectionUserRepository.countBySectionId(sectionId)).thenReturn(0L);
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                sectionService.leaveCurrentSection(sectionId);

                verify(sectionRepository).deleteAllById(List.of(sectionId));
        }

        @Test
        void leaveCurrentSection_leaderWithOtherMembers_throwsBusinessException() {
                Long sectionId = 10L;
                Long leaderUserId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(leaderUserId);

                SectionUser leader = new SectionUser();
                leader.setSectionId(sectionId);
                leader.setUserId(leaderUserId);
                SectionUser other = new SectionUser();
                other.setSectionId(sectionId);
                other.setUserId(101L);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId))
                                .thenReturn(List.of(leader, other));

                Role leaderRole = Role.builder()
                                .id(501L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("Leader")
                                .build();
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Leader"))
                                .thenReturn(Optional.of(leaderRole));
                when(userRoleRepository.existsById(
                                io.github.Romariok.orkestro.user.models.UserRoleId.builder()
                                                .userId(leaderUserId)
                                                .roleId(leaderRole.getId())
                                                .build()))
                                .thenReturn(true);

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.leaveCurrentSection(sectionId));

                verify(sectionUserRepository, never()).deleteBySectionIdInAndUserId(any(), any());
        }

        @Test
        void leaveCurrentSection_coLeaderWithOtherMembersBeyondLeader_throwsBusinessException() {
                Long sectionId = 10L;
                Long coLeaderUserId = 101L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(coLeaderUserId);

                SectionUser leader = new SectionUser();
                leader.setSectionId(sectionId);
                leader.setUserId(100L);
                SectionUser coLeader = new SectionUser();
                coLeader.setSectionId(sectionId);
                coLeader.setUserId(coLeaderUserId);
                SectionUser other = new SectionUser();
                other.setSectionId(sectionId);
                other.setUserId(102L);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId))
                                .thenReturn(List.of(leader, coLeader, other));

                Role coLeaderRole = Role.builder()
                                .id(502L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("Co-leader")
                                .build();
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Leader"))
                                .thenReturn(Optional.empty());
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Co-leader"))
                                .thenReturn(Optional.of(coLeaderRole));
                when(userRoleRepository.existsById(
                                io.github.Romariok.orkestro.user.models.UserRoleId.builder()
                                                .userId(coLeaderUserId)
                                                .roleId(coLeaderRole.getId())
                                                .build()))
                                .thenReturn(true);

                assertThrows(
                                BusinessException.class,
                                () -> sectionService.leaveCurrentSection(sectionId));

                verify(sectionUserRepository, never()).deleteBySectionIdInAndUserId(any(), any());
        }

        @Test
        void addUserToSection_secondMember_becomesCoLeader() {
                Long sectionId = 10L;
                Long organizationId = 1L;
                Long firstUserId = 100L;
                Long secondUserId = 101L;

                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(organizationId);
                section.setParentSectionId(null);

                when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, secondUserId))
                                .thenReturn(Optional.empty());

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(secondUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, secondUserId))
                                .thenReturn(Optional.of(membership));

                when(sectionRepository.existsById(sectionId)).thenReturn(true);

                SectionUser first = new SectionUser();
                first.setSectionId(sectionId);
                first.setUserId(firstUserId);
                SectionUser second = new SectionUser();
                second.setSectionId(sectionId);
                second.setUserId(secondUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId))
                                .thenReturn(List.of(first, second));

                Role leaderRole = Role.builder().id(501L).scope(RoleScopeType.SECTION).sectionId(sectionId)
                                .name("Leader").build();
                Role coLeaderRole = Role.builder().id(502L).scope(RoleScopeType.SECTION).sectionId(sectionId)
                                .name("Co-leader")
                                .build();
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Leader"))
                                .thenReturn(Optional.of(leaderRole));
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, sectionId, "Co-leader"))
                                .thenReturn(Optional.of(coLeaderRole));

                sectionService.addUserToSection(sectionId, secondUserId);

                verify(userRoleRepository).deleteByRoleId(leaderRole.getId());
                verify(userRoleRepository).deleteByRoleId(coLeaderRole.getId());

                ArgumentCaptor<UserRole> roleCaptor = ArgumentCaptor.forClass(UserRole.class);
                verify(userRoleRepository, org.mockito.Mockito.times(2)).save(roleCaptor.capture());
                List<UserRole> saved = roleCaptor.getAllValues();

                // leader -> first user, co-leader -> second user
                boolean hasLeader = saved.stream()
                                .anyMatch(ur -> ur.getUserId().equals(firstUserId)
                                                && ur.getRoleId().equals(leaderRole.getId()));
                boolean hasCoLeader = saved.stream()
                                .anyMatch(ur -> ur.getUserId().equals(secondUserId)
                                                && ur.getRoleId().equals(coLeaderRole.getId()));
                assertEquals(true, hasLeader);
                assertEquals(true, hasCoLeader);
        }

        @Test
        void searchMembers_blankQuery_returnsAll() {
                Long sectionId = 10L;
                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                SectionUser membership = new SectionUser();
                membership.setSectionId(sectionId);
                membership.setUserId(100L);

                Page<SectionUser> page = new PageImpl<>(List.of(membership), PageRequest.of(0, 20), 1);
                when(sectionUserRepository.findAll(
                                org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<SectionUser>>any(),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(page);
                when(sectionMemberMapper.toDto(any(SectionUser.class)))
                                .thenReturn(new io.github.Romariok.orkestro.section.dto.SectionMemberDTO(
                                                100L, "u", "User", 1L, java.time.Instant.parse("2026-01-01T00:00:00Z"),
                                                null));

                Page<io.github.Romariok.orkestro.section.dto.SectionMemberDTO> result = sectionService.searchMembers(
                                sectionId, "   ", List.of(), List.of(), PageRequest.of(0, 20));

                assertEquals(1, result.getTotalElements());
        }

        @Test
        void createSectionInSection_depthExceeds10_throwsSectionDepthExceededException() {
                Section[] chain = new Section[10];
                for (int i = 0; i < 10; i++) {
                        chain[i] = new Section();
                        chain[i].setId((long) (i + 1));
                        chain[i].setOrganizationId(1L);
                        chain[i].setParentSectionId(i == 0 ? null : (long) i);
                }
                Long parentId = 10L;
                when(sectionRepository.findById(parentId)).thenReturn(Optional.of(chain[9]));
                for (int i = 8; i >= 0; i--) {
                        when(sectionRepository.findById((long) (i + 1))).thenReturn(Optional.of(chain[i]));
                }

                SectionCreateRequestDTO request = new SectionCreateRequestDTO("Child", "desc");

                assertThrows(
                                SectionDepthExceededException.class,
                                () -> sectionService.createSectionInSection(parentId, request));

                verify(sectionRepository, never()).save(any());
        }

        @Test
        void createSectionInSection_depth9_succeeds() {
                Section[] chain = new Section[9];
                for (int i = 0; i < 9; i++) {
                        chain[i] = new Section();
                        chain[i].setId((long) (i + 1));
                        chain[i].setOrganizationId(1L);
                        chain[i].setParentSectionId(i == 0 ? null : (long) i);
                }
                Long parentId = 9L;
                when(sectionRepository.findById(parentId)).thenReturn(Optional.of(chain[8]));
                for (int i = 7; i >= 0; i--) {
                        when(sectionRepository.findById((long) (i + 1))).thenReturn(Optional.of(chain[i]));
                }

                when(sectionRepository.existsByOrganizationIdAndParentSectionIdAndName(1L, parentId, "NewSection"))
                                .thenReturn(false);

                Section saved = new Section();
                saved.setId(100L);
                saved.setName("NewSection");
                saved.setOrganizationId(1L);
                saved.setParentSectionId(parentId);
                when(sectionRepository.save(any(Section.class))).thenReturn(saved);

                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, 100L)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSystemTrue(RoleScopeType.SECTION)).thenReturn(List.of());
                when(securityUtils.getCurrentUserId()).thenReturn(200L);
                when(sectionUserRepository.findBySectionIdAndUserId(100L, 200L)).thenReturn(Optional.empty());
                when(sectionRepository.existsById(100L)).thenReturn(true);
                SectionUser creator = new SectionUser();
                creator.setSectionId(100L);
                creator.setUserId(200L);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(100L)).thenReturn(List.of(creator));
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 100L, "Leader"))
                                .thenReturn(Optional.empty());
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION, 100L, "Co-leader"))
                                .thenReturn(Optional.empty());
                when(sectionMapper.toDto(saved))
                                .thenReturn(new SectionDTO(100L, "NewSection", null, 1L, parentId));

                SectionDTO result = sectionService.createSectionInSection(parentId,
                                new SectionCreateRequestDTO("NewSection", null));

                assertEquals(100L, result.getId());
                verify(sectionRepository).save(any(Section.class));
        }

        @Test
        void leaveCurrentSection_lastMember_deletesEventSectionRows() {
                Long sectionId = 10L;
                Long currentUserId = 100L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                SectionUser member = new SectionUser();
                member.setSectionId(sectionId);
                member.setUserId(currentUserId);
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId)).thenReturn(List.of(member));
                when(sectionRepository.findByParentSectionId(sectionId)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSectionIdIn(eq(RoleScopeType.SECTION), any()))
                                .thenReturn(List.of());
                when(sectionUserRepository.countBySectionId(sectionId)).thenReturn(0L);
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                sectionService.leaveCurrentSection(sectionId);

                verify(eventSectionRepository).deleteBySectionId(sectionId);
                verify(sectionRepository).deleteAllById(List.of(sectionId));
        }

        @Test
        void searchMembers_sortByJoinedAt_mapsSortToMembership() {
                Long sectionId = 10L;
                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(roleRepository.findByScopeAndSectionId(RoleScopeType.SECTION, sectionId)).thenReturn(List.of());

                when(sectionUserRepository.findAll(
                                org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<SectionUser>>any(),
                                any(org.springframework.data.domain.Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

                sectionService.searchMembers(
                                sectionId,
                                null,
                                List.of(),
                                List.of(),
                                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("joinedAt"))));

                ArgumentCaptor<org.springframework.data.domain.Pageable> captor = ArgumentCaptor
                                .forClass(org.springframework.data.domain.Pageable.class);
                verify(sectionUserRepository).findAll(
                                org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<SectionUser>>any(),
                                captor.capture());

                Sort.Order order = captor.getValue().getSort().getOrderFor("joinedAt");
                org.junit.jupiter.api.Assertions.assertNotNull(order);
                org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        void leaveCurrentSection_userNotMember_completesWithoutException() {
                Long sectionId = 10L;
                Long currentUserId = 200L;

                when(sectionRepository.existsById(sectionId)).thenReturn(true);
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                // No members in section — user is not a member
                when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(sectionId)).thenReturn(List.of());

                // Leaf section — no children
                when(sectionRepository.findByParentSectionId(sectionId)).thenReturn(List.of());
                when(roleRepository.findByScopeAndSectionIdIn(eq(RoleScopeType.SECTION), any()))
                                .thenReturn(List.of());
                // After no-op deletion, still has other members (no cascade delete)
                when(sectionUserRepository.countBySectionId(sectionId)).thenReturn(1L);

                // Should complete without throwing any exception
                sectionService.leaveCurrentSection(sectionId);

                // Deletion is called but has no effect (deletes 0 rows in practice)
                verify(sectionUserRepository).deleteBySectionIdInAndUserId(any(), eq(currentUserId));
                // Section is NOT deleted because there are still members
                verify(sectionRepository, never()).deleteAllById(any());
        }
}
