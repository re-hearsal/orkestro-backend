package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dto.user.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.models.role.Permission;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.user.User;
import io.github.Romariok.orkestro.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private UserRoleRepository userRoleRepository;

        @Mock
        private RolePermissionRepository rolePermissionRepository;

        @Mock
        private UserInstrumentRepository userInstrumentRepository;

        @InjectMocks
        private UserService userService;

        @Test
        void loadUserByUsername_userNotFound_throwsException() {
                String username = "unknown";
                when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

                assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(username));
        }

        @Test
        void loadUserByUsername_withOrganizationRole_buildsContextualAuthorities() {
                // given
                User user = User.builder()
                                .id(1L)
                                .username("user")
                                .password("password")
                                .build();

                Role role = Role.builder()
                                .id(10L)
                                .name("Leader")
                                .scope(RoleScopeType.ORGANIZATION)
                                .organizationId(100L)
                                .build();

                Permission permission = Permission.builder()
                                .code("ORG_EDIT")
                                .description("Edit organization")
                                .build();

                when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
                when(userRoleRepository.findRolesByUserId(1L)).thenReturn(List.of(role));
                when(rolePermissionRepository.findPermissionsByRoleId(10L)).thenReturn(List.of(permission));

                UserDetails userDetails = userService.loadUserByUsername("user");
                Set<String> authorityStrings = userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(java.util.stream.Collectors.toSet());

                assertTrue(authorityStrings.contains("CTX_ROLE_ORG:100:Leader"));
                assertTrue(authorityStrings.contains("CTX_PERM_ORG:100:ORG_EDIT"));
                assertEquals(2, authorityStrings.size());
        }

        @Test
        void loadUserByUsername_withSectionRole_buildsContextualAuthorities() {
                User user = User.builder()
                                .id(2L)
                                .username("section-user")
                                .password("password")
                                .build();

                Role role = Role.builder()
                                .id(20L)
                                .name("SectionLeader")
                                .scope(RoleScopeType.SECTION)
                                .sectionId(5L)
                                .build();

                Permission permission = Permission.builder()
                                .code("SECTION_EDIT")
                                .description("Edit section")
                                .build();

                when(userRepository.findByUsername("section-user")).thenReturn(Optional.of(user));
                when(userRoleRepository.findRolesByUserId(2L)).thenReturn(List.of(role));
                when(rolePermissionRepository.findPermissionsByRoleId(20L)).thenReturn(List.of(permission));

                UserDetails userDetails = userService.loadUserByUsername("section-user");
                Set<String> authorityStrings = userDetails.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(java.util.stream.Collectors.toSet());

                assertTrue(authorityStrings.contains("CTX_ROLE_SECTION:5:SectionLeader"));
                assertTrue(authorityStrings.contains("CTX_PERM_SECTION:5:SECTION_EDIT"));
                assertEquals(2, authorityStrings.size());
        }

        @Test
        void searchUsers_noFilters_returnsAllUsers() {
                User u1 = User.builder().id(1L).name("Alice").build();
                User u2 = User.builder().id(2L).name("Bob").build();
                when(userRepository.findAll()).thenReturn(List.of(u1, u2));

                var result = userService.searchUsers(null, null);

                assertEquals(2, result.size());
                assertEquals(List.of(u1, u2), result);
                verify(userRepository).findAll();
                verify(userRepository, never()).findByNameContainingIgnoreCase(org.mockito.Mockito.anyString());
        }

        @Test
        void searchUsers_nameOnly_usesNameFilterCaseInsensitive() {
                User u1 = User.builder().id(1L).name("Alice").build();
                when(userRepository.findByNameContainingIgnoreCase("Ali")).thenReturn(List.of(u1));

                var result = userService.searchUsers("  Ali  ", null);

                assertEquals(1, result.size());
                assertEquals("Alice", result.getFirst().getName());
                verify(userRepository).findByNameContainingIgnoreCase("Ali");
                verify(userRepository, never()).findAll();
        }

        @Test
        void searchUsers_rolesOnly_usesRoleFilter() {
                User u1 = User.builder().id(1L).name("Alice").build();
                List<Long> roleIds = List.of(10L, 20L);
                when(userRepository.findByNameAndRoleIds(null, roleIds)).thenReturn(List.of(u1));

                var result = userService.searchUsers(null, roleIds);

                assertEquals(1, result.size());
                assertEquals("Alice", result.getFirst().getName());
                verify(userRepository).findByNameAndRoleIds(null, roleIds);
                verify(userRepository, never()).findAll();
                verify(userRepository, never()).findByNameContainingIgnoreCase(org.mockito.Mockito.anyString());
        }

        @Test
        void searchUsers_nameAndRoles_filtersByBoth() {
                User u1 = User.builder().id(1L).name("Alice Leader").build();
                List<Long> roleIds = List.of(10L);
                when(userRepository.findByNameAndRoleIds("Alice", roleIds)).thenReturn(List.of(u1));

                var result = userService.searchUsers("Alice", roleIds);

                assertEquals(1, result.size());
                assertEquals("Alice Leader", result.getFirst().getName());
                verify(userRepository).findByNameAndRoleIds("Alice", roleIds);
                verify(userRepository, never()).findAll();
                verify(userRepository, never()).findByNameContainingIgnoreCase(org.mockito.Mockito.anyString());
        }

        @Test
        void updateUserProfile_updatesNonNullFieldsAndTimestamp() {
                Long userId = 1L;
                User user = User.builder()
                                .id(userId)
                                .name("Old Name")
                                .email("old@example.com")
                                .location("Old City")
                                .birthDate(LocalDate.of(1990, 1, 1))
                                .notificationChannel(NotificationChannelType.EMAIL)
                                .updatedAt(Instant.parse("2020-01-01T00:00:00Z"))
                                .build();

                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userRepository.save(org.mockito.Mockito.any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                UserProfileUpdateRequestDTO request = new UserProfileUpdateRequestDTO();
                request.setName("New Name");
                request.setEmail("new@example.com");
                request.setLocation("New City");
                request.setBirthDate(LocalDate.of(1995, 5, 5));
                request.setNotificationChannel(NotificationChannelType.TELEGRAM);

                User result = userService.updateUserProfile(userId, request);

                assertEquals("New Name", result.getName());
                assertEquals("new@example.com", result.getEmail());
                assertEquals("New City", result.getLocation());
                assertEquals(LocalDate.of(1995, 5, 5), result.getBirthDate());
                assertEquals(NotificationChannelType.TELEGRAM, result.getNotificationChannel());
                assertTrue(result.getUpdatedAt() != null);
                verify(userRepository).save(user);
        }

        @Test
        void deleteUserAccount_existingUser_cleansRelationsAndDeletesUser() {
                Long userId = 1L;
                when(userRepository.existsById(userId)).thenReturn(true);

                userService.deleteUserAccount(userId);

                verify(userInstrumentRepository).deleteByUserId(userId);
                verify(userRoleRepository).deleteByUserId(userId);
                verify(userRepository).deleteById(userId);
        }

        @Test
        void deleteUserAccount_userNotFound_throwsEntityNotFound() {
                Long userId = 1L;
                when(userRepository.existsById(userId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> userService.deleteUserAccount(userId));

                verify(userInstrumentRepository, never()).deleteByUserId(org.mockito.Mockito.any());
                verify(userRoleRepository, never()).deleteByUserId(org.mockito.Mockito.any());
                verify(userRepository, never()).deleteById(org.mockito.Mockito.any());
        }
}
