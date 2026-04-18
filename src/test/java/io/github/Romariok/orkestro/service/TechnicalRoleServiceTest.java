package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.user.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleCreateRequestDTO;
import io.github.Romariok.orkestro.user.dto.TechnicalRoleDTO;
import io.github.Romariok.orkestro.user.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.RolePermission;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.UserRoleId;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.PermissionRepository;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.user.service.TechnicalRoleService;
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
class TechnicalRoleServiceTest {

        @Mock
        private TechnicalRoleDao technicalRoleDao;

        @Mock
        private TechnicalRoleMapper technicalRoleMapper;

        @Mock
        private UserRoleRepository userRoleRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private OrganizationUserRepository organizationUserRepository;

        @Mock
        private SectionUserRepository sectionUserRepository;

        @Mock
        private RolePermissionRepository rolePermissionRepository;

        @Mock
        private PermissionRepository permissionRepository;

        @InjectMocks
        private TechnicalRoleService technicalRoleService;

        @Test
        void getUserRoles_returnsMappedDtos() {
                Long userId = 1L;
                Role role = Role.builder()
                                .id(10L)
                                .build();
                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(10L);

                when(technicalRoleDao.findUserRoles(userId)).thenReturn(List.of(role));
                when(technicalRoleMapper.toDto(role)).thenReturn(dto);
                when(rolePermissionRepository.findPermissionsByRoleId(10L)).thenReturn(List.of());

                List<TechnicalRoleDTO> result = technicalRoleService.getUserRoles(userId);

                assertEquals(1, result.size());
                assertEquals(10L, result.getFirst().getId());
        }

        @Test
        void createOrganizationRole_success_savesRoleAndPermissions() {
                Long organizationId = 100L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "Editor",
                                List.of("ORG_EDIT", "REPERTOIRE_CREATE_SONG"));

                when(roleRepository.findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION,
                                organizationId, "Editor"))
                                .thenReturn(Optional.empty());

                Role savedRole = Role.builder()
                                .id(10L)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("Editor")
                                .build();
                when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

                Permission p1 = Permission.builder().code("ORG_EDIT").description("d").build();
                Permission p2 = Permission.builder().code("REPERTOIRE_CREATE_SONG").description("d").build();
                when(permissionRepository.findByCodeIn(any())).thenReturn(List.of(p1, p2));

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(10L);
                when(technicalRoleMapper.toDto(savedRole)).thenReturn(dto);

                TechnicalRoleDTO result = technicalRoleService.createOrganizationRole(organizationId, request);

                assertEquals(10L, result.getId());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<RolePermission>> captor = (ArgumentCaptor<List<RolePermission>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(rolePermissionRepository).saveAll(captor.capture());
                List<RolePermission> stored = captor.getValue();
                assertEquals(2, stored.size());
        }

        @Test
        void createOrganizationRole_duplicateName_throwsBusinessException() {
                Long organizationId = 100L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "Editor",
                                List.of("ORG_EDIT"));

                Role existing = Role.builder()
                                .id(5L)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("Editor")
                                .build();
                when(roleRepository.findByScopeAndOrganizationIdAndName(RoleScopeType.ORGANIZATION,
                                organizationId, "Editor"))
                                .thenReturn(Optional.of(existing));

                assertThrows(
                                BusinessException.class,
                                () -> technicalRoleService.createOrganizationRole(organizationId, request));

                verify(roleRepository, never()).save(any(Role.class));
                verify(rolePermissionRepository, never()).saveAll(any());
        }

        @Test
        void createSectionRole_success_savesRoleAndPermissions() {
                Long sectionId = 5L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "SectionEditor",
                                List.of("SECTION_EDIT"));

                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION,
                                sectionId, "SectionEditor"))
                                .thenReturn(Optional.empty());

                Role savedRole = Role.builder()
                                .id(20L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("SectionEditor")
                                .build();
                when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

                Permission p = Permission.builder().code("SECTION_EDIT").description("d").build();
                when(permissionRepository.findByCodeIn(any())).thenReturn(List.of(p));

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(20L);
                when(technicalRoleMapper.toDto(savedRole)).thenReturn(dto);

                TechnicalRoleDTO result = technicalRoleService.createSectionRole(sectionId, request);

                assertEquals(20L, result.getId());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<RolePermission>> captor = (ArgumentCaptor<List<RolePermission>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(rolePermissionRepository).saveAll(captor.capture());
                List<RolePermission> stored = captor.getValue();
                assertEquals(1, stored.size());
        }

        @Test
        void createSectionRole_duplicateName_throwsBusinessException() {
                Long sectionId = 5L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "SectionEditor",
                                List.of("SECTION_EDIT"));

                Role existing = Role.builder()
                                .id(21L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("SectionEditor")
                                .build();
                when(roleRepository.findByScopeAndSectionIdAndName(RoleScopeType.SECTION,
                                sectionId, "SectionEditor"))
                                .thenReturn(Optional.of(existing));

                assertThrows(
                                BusinessException.class,
                                () -> technicalRoleService.createSectionRole(sectionId, request));

                verify(roleRepository, never()).save(any(Role.class));
                verify(rolePermissionRepository, never()).saveAll(any());
        }

        @Test
        void assignOrganizationRoleToUser_userNotFound_throwsEntityNotFound() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 10L;
                when(userRepository.existsById(userId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> technicalRoleService.assignOrganizationRoleToUser(organizationId, userId,
                                                roleId));
        }

        @Test
        void assignOrganizationRoleToUser_roleNotFound_throwsEntityNotFound() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 10L;

                when(userRepository.existsById(userId)).thenReturn(true);
                when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> technicalRoleService.assignOrganizationRoleToUser(organizationId, userId,
                                                roleId));
        }

        @Test
        void assignOrganizationRoleToUser_roleAlreadyAssigned_doesNotSave() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 10L;

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .build();

                when(userRepository.existsById(userId)).thenReturn(true);
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                when(userRoleRepository.existsById(any(UserRoleId.class))).thenReturn(true);

                technicalRoleService.assignOrganizationRoleToUser(organizationId, userId, roleId);

                verify(userRoleRepository, never()).save(any(UserRole.class));
        }

        @Test
        void assignOrganizationRoleToUser_success_savesUserRole() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 10L;

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .build();

                when(userRepository.existsById(userId)).thenReturn(true);
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                when(userRoleRepository.existsById(any(UserRoleId.class))).thenReturn(false);

                technicalRoleService.assignOrganizationRoleToUser(organizationId, userId, roleId);

                ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
                verify(userRoleRepository).save(captor.capture());
                UserRole saved = captor.getValue();

                assertEquals(userId, saved.getUserId());
                assertEquals(roleId, saved.getRoleId());
        }

        @Test
        void removeOrganizationRoleFromUser_deletesUserRole() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 10L;

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                technicalRoleService.removeOrganizationRoleFromUser(organizationId, userId, roleId);

                ArgumentCaptor<UserRoleId> captor = ArgumentCaptor.forClass(UserRoleId.class);
                verify(userRoleRepository).deleteById(captor.capture());
                UserRoleId id = captor.getValue();

                assertEquals(userId, id.getUserId());
                assertEquals(roleId, id.getRoleId());
        }

        @Test
        void assignSectionRoleToUser_success_savesUserRole() {
                Long sectionId = 5L;
                Long userId = 1L;
                Long roleId = 20L;

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .build();

                when(userRepository.existsById(userId)).thenReturn(true);
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                SectionUser su = new SectionUser();
                su.setSectionId(sectionId);
                su.setUserId(userId);
                when(sectionUserRepository.findBySectionIdAndUserId(sectionId, userId))
                                .thenReturn(Optional.of(su));

                when(userRoleRepository.existsById(any(UserRoleId.class))).thenReturn(false);

                technicalRoleService.assignSectionRoleToUser(sectionId, userId, roleId);

                ArgumentCaptor<UserRole> captor = ArgumentCaptor.forClass(UserRole.class);
                verify(userRoleRepository).save(captor.capture());
                UserRole saved = captor.getValue();

                assertEquals(userId, saved.getUserId());
                assertEquals(roleId, saved.getRoleId());
        }

        @Test
        void removeSectionRoleFromUser_deletesUserRole() {
                Long sectionId = 5L;
                Long userId = 1L;
                Long roleId = 20L;

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                technicalRoleService.removeSectionRoleFromUser(sectionId, userId, roleId);

                ArgumentCaptor<UserRoleId> captor = ArgumentCaptor.forClass(UserRoleId.class);
                verify(userRoleRepository).deleteById(captor.capture());
                UserRoleId id = captor.getValue();

                assertEquals(userId, id.getUserId());
                assertEquals(roleId, id.getRoleId());
        }

        @Test
        void deleteOrganizationRole_success_deletesRolePermissionsAndRole() {
                Long organizationId = 100L;
                Long roleId = 10L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .system(false)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(userRoleRepository.existsByRoleId(roleId)).thenReturn(false);

                technicalRoleService.deleteOrganizationRole(organizationId, roleId);

                verify(rolePermissionRepository).deleteByRoleId(roleId);
                verify(roleRepository).delete(role);
        }

        @Test
        void deleteOrganizationRole_systemRole_throwsBusinessException() {
                Long organizationId = 100L;
                Long roleId = 10L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .system(true)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                assertThrows(BusinessException.class,
                                () -> technicalRoleService.deleteOrganizationRole(organizationId, roleId));

                verify(rolePermissionRepository, never()).deleteByRoleId(roleId);
                verify(roleRepository, never()).delete(any(Role.class));
        }

        @Test
        void deleteOrganizationRole_assignedToUsers_throwsBusinessException() {
                Long organizationId = 100L;
                Long roleId = 10L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .system(false)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(userRoleRepository.existsByRoleId(roleId)).thenReturn(true);

                assertThrows(BusinessException.class,
                                () -> technicalRoleService.deleteOrganizationRole(organizationId, roleId));

                verify(rolePermissionRepository, never()).deleteByRoleId(roleId);
                verify(roleRepository, never()).delete(any(Role.class));
        }

        @Test
        void deleteSectionRole_success_deletesRolePermissionsAndRole() {
                Long sectionId = 5L;
                Long roleId = 20L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .system(false)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(userRoleRepository.existsByRoleId(roleId)).thenReturn(false);

                technicalRoleService.deleteSectionRole(sectionId, roleId);

                verify(rolePermissionRepository).deleteByRoleId(roleId);
                verify(roleRepository).delete(role);
        }

        @Test
        void deleteSectionRole_systemRole_throwsBusinessException() {
                Long sectionId = 5L;
                Long roleId = 20L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .system(true)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                assertThrows(BusinessException.class,
                                () -> technicalRoleService.deleteSectionRole(sectionId, roleId));

                verify(rolePermissionRepository, never()).deleteByRoleId(roleId);
                verify(roleRepository, never()).delete(any(Role.class));
        }

        @Test
        void deleteSectionRole_assignedToUsers_throwsBusinessException() {
                Long sectionId = 5L;
                Long roleId = 20L;
                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .system(false)
                                .build();
                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(userRoleRepository.existsByRoleId(roleId)).thenReturn(true);

                assertThrows(BusinessException.class,
                                () -> technicalRoleService.deleteSectionRole(sectionId, roleId));

                verify(rolePermissionRepository, never()).deleteByRoleId(roleId);
                verify(roleRepository, never()).delete(any(Role.class));
        }

        @Test
        void getOrganizationRoles_returnsMappedDtos() {
                Long organizationId = 100L;
                Role role = Role.builder()
                                .id(10L)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("Editor")
                                .build();

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(10L);

                when(technicalRoleDao.findOrganizationRoles(organizationId)).thenReturn(List.of(role));
                when(technicalRoleMapper.toDto(role)).thenReturn(dto);
                when(rolePermissionRepository.findPermissionsByRoleId(10L)).thenReturn(List.of());

                List<TechnicalRoleDTO> result = technicalRoleService.getOrganizationRoles(organizationId);

                assertEquals(1, result.size());
                assertEquals(10L, result.getFirst().getId());
        }

        @Test
        void getSectionRoles_returnsMappedDtos() {
                Long sectionId = 5L;
                Role role = Role.builder()
                                .id(20L)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("SectionEditor")
                                .build();

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(20L);

                when(technicalRoleDao.findSectionRoles(sectionId)).thenReturn(List.of(role));
                when(technicalRoleMapper.toDto(role)).thenReturn(dto);
                when(rolePermissionRepository.findPermissionsByRoleId(20L)).thenReturn(List.of());

                List<TechnicalRoleDTO> result = technicalRoleService.getSectionRoles(sectionId);

                assertEquals(1, result.size());
                assertEquals(20L, result.getFirst().getId());
        }

        @Test
        void updateOrganizationRole_success_updatesNameAndPermissions() {
                Long organizationId = 100L;
                Long roleId = 10L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "NewName", List.of("ORG_EDIT"));

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("OldName")
                                .system(false)
                                .build();

                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(roleRepository.findByScopeAndOrganizationIdAndName(
                                RoleScopeType.ORGANIZATION, organizationId, "NewName"))
                                .thenReturn(Optional.empty());

                Permission p = Permission.builder().code("ORG_EDIT").description("d").build();
                when(permissionRepository.findByCodeIn(any())).thenReturn(List.of(p));

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(roleId);
                when(technicalRoleMapper.toDto(role)).thenReturn(dto);
                when(rolePermissionRepository.findPermissionsByRoleId(roleId)).thenReturn(List.of());

                TechnicalRoleDTO result = technicalRoleService.updateOrganizationRole(organizationId, roleId, request);

                assertEquals(roleId, result.getId());
                assertEquals("NewName", role.getName());
                verify(rolePermissionRepository).deleteByRoleId(roleId);
                verify(rolePermissionRepository).saveAll(any());
        }

        @Test
        void updateOrganizationRole_notFound_throwsEntityNotFoundException() {
                Long organizationId = 100L;
                Long roleId = 99L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO("Name", List.of());

                when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> technicalRoleService.updateOrganizationRole(organizationId, roleId, request));
        }

        @Test
        void updateOrganizationRole_duplicateName_throwsBusinessException() {
                Long organizationId = 100L;
                Long roleId = 10L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO("ExistingName", List.of());

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("OldName")
                                .system(false)
                                .build();

                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

                Role existing = Role.builder()
                                .id(99L)
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(organizationId)
                                .name("ExistingName")
                                .build();
                when(roleRepository.findByScopeAndOrganizationIdAndName(
                                RoleScopeType.ORGANIZATION, organizationId, "ExistingName"))
                                .thenReturn(Optional.of(existing));

                assertThrows(
                                BusinessException.class,
                                () -> technicalRoleService.updateOrganizationRole(organizationId, roleId, request));
        }

        @Test
        void updateSectionRole_success_updatesNameAndPermissions() {
                Long sectionId = 5L;
                Long roleId = 20L;
                TechnicalRoleCreateRequestDTO request = new TechnicalRoleCreateRequestDTO(
                                "UpdatedSectionRole", List.of("SECTION_EDIT"));

                Role role = Role.builder()
                                .id(roleId)
                                .scope(RoleScopeType.SECTION)
                                .sectionId(sectionId)
                                .name("OldSectionRole")
                                .system(false)
                                .build();

                when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
                when(roleRepository.findByScopeAndSectionIdAndName(
                                RoleScopeType.SECTION, sectionId, "UpdatedSectionRole"))
                                .thenReturn(Optional.empty());

                Permission p = Permission.builder().code("SECTION_EDIT").description("d").build();
                when(permissionRepository.findByCodeIn(any())).thenReturn(List.of(p));

                TechnicalRoleDTO dto = new TechnicalRoleDTO();
                dto.setId(roleId);
                when(technicalRoleMapper.toDto(role)).thenReturn(dto);
                when(rolePermissionRepository.findPermissionsByRoleId(roleId)).thenReturn(List.of());

                TechnicalRoleDTO result = technicalRoleService.updateSectionRole(sectionId, roleId, request);

                assertEquals(roleId, result.getId());
                verify(rolePermissionRepository).deleteByRoleId(roleId);
        }

        @Test
        void removeOrganizationRoleFromUser_roleNotFound_throwsEntityNotFoundException() {
                Long organizationId = 100L;
                Long userId = 1L;
                Long roleId = 99L;

                when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> technicalRoleService.removeOrganizationRoleFromUser(organizationId, userId, roleId));
        }
}
