package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.dto.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
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
import org.springframework.mock.web.MockMultipartFile;

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

        @Mock
        private StoredFileRepository storedFileRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private FileReferenceService fileReferenceService;

        @Mock
        private FileLimitsProperties fileLimitsProperties;

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
                                .preferredLanguage(UserLanguageType.RU)
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
                request.setPreferredLanguage(UserLanguageType.EN);

                User result = userService.updateUserProfile(userId, request);

                assertEquals("New Name", result.getName());
                assertEquals("new@example.com", result.getEmail());
                assertEquals("New City", result.getLocation());
                assertEquals(LocalDate.of(1995, 5, 5), result.getBirthDate());
                assertEquals(NotificationChannelType.EMAIL, result.getNotificationChannel());
                assertEquals(UserLanguageType.EN, result.getPreferredLanguage());
                assertTrue(result.getUpdatedAt() != null);
                verify(userRepository).save(user);
        }

        @Test
        void deleteUserAccount_existingUser_cleansRelationsAndDeletesUser() {
                Long userId = 1L;
                when(userRepository.existsById(userId)).thenReturn(true);

                userService.deleteUserAccount(userId);

                verify(storedFileRepository).clearUploadedByUserId(userId);
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

                verify(storedFileRepository, never()).clearUploadedByUserId(org.mockito.Mockito.any());
                verify(userInstrumentRepository, never()).deleteByUserId(org.mockito.Mockito.any());
                verify(userRoleRepository, never()).deleteByUserId(org.mockito.Mockito.any());
                verify(userRepository, never()).deleteById(org.mockito.Mockito.any());
        }

        @Test
        void updateNotificationChannel_telegram_throwsBusinessException() {
                Long userId = 1L;
                User user = User.builder().id(userId).notificationChannel(NotificationChannelType.EMAIL).build();
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));

                assertThrows(
                                BusinessException.class,
                                () -> userService.updateNotificationChannel(userId, NotificationChannelType.TELEGRAM));

                verify(userRepository, never()).save(org.mockito.Mockito.any(User.class));
        }

        @Test
        void updateNotificationChannel_email_clearsTelegramUserIdAndSaves() {
                Long userId = 1L;
                User user = User.builder()
                                .id(userId)
                                .notificationChannel(NotificationChannelType.TELEGRAM)
                                .telegramUserId(123456789L)
                                .updatedAt(Instant.parse("2020-01-01T00:00:00Z"))
                                .build();
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(userRepository.save(org.mockito.Mockito.any(User.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                User result = userService.updateNotificationChannel(userId, NotificationChannelType.EMAIL);

                assertEquals(NotificationChannelType.EMAIL, result.getNotificationChannel());
                assertEquals(null, result.getTelegramUserId());
                assertTrue(result.getUpdatedAt() != null);
                verify(userRepository).save(user);
        }

        @Test
        void updateProfileImage_nonImageFile_throwsBusinessException() {
                Long userId = 1L;
                User user = User.builder().id(userId).build();
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "notes.txt",
                                "text/plain",
                                "hello".getBytes());

                assertThrows(BusinessException.class, () -> userService.updateProfileImage(userId, file));

                verify(fileStorageService, never()).upload(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.any());
                verify(userRepository, never()).save(org.mockito.Mockito.any(User.class));
        }

        @Test
        void deleteProfileImage_existingImage_deletesFileAndClearsReference() {
                Long userId = 1L;
                User user = User.builder().id(userId).profileImageFileId(10L).build();
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(fileReferenceService.isFileReferenced(10L)).thenReturn(false);

                userService.deleteProfileImage(userId);

                verify(fileStorageService).delete(10L);
                assertEquals(null, user.getProfileImageFileId());
                verify(userRepository).save(user);
        }

        @Test
        void updateProfileImage_replacesOldAndDeletesOnlyIfUnreferenced() {
                Long userId = 1L;
                User user = User.builder().id(userId).profileImageFileId(10L).build();
                when(userRepository.findById(userId)).thenReturn(Optional.of(user));
                when(fileStorageService.upload(org.mockito.Mockito.any(), org.mockito.Mockito.any(), org.mockito.Mockito.eq(userId)))
                                .thenReturn(StoredFile.builder().id(20L).build());
                when(fileReferenceService.isFileReferenced(10L)).thenReturn(false);

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "avatar.png",
                                "image/png",
                                "img".getBytes());
                userService.updateProfileImage(userId, file);

                assertEquals(20L, user.getProfileImageFileId());
                verify(fileStorageService).delete(10L);
        }
}
