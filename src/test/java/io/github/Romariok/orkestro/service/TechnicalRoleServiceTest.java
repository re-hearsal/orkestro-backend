package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dao.TechnicalRoleDao;
import io.github.Romariok.orkestro.dto.role.TechnicalRoleDTO;
import io.github.Romariok.orkestro.mapper.TechnicalRoleMapper;
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.section.SectionUser;
import io.github.Romariok.orkestro.models.user.UserRole;
import io.github.Romariok.orkestro.models.user.UserRoleId;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.SectionUserRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
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
                when(technicalRoleMapper.toDtoList(List.of(role))).thenReturn(List.of(dto));

                List<TechnicalRoleDTO> result = technicalRoleService.getUserRoles(userId);

                assertEquals(1, result.size());
                assertEquals(10L, result.getFirst().getId());
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
}
