package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.models.Permission;
import io.github.Romariok.orkestro.models.Role;
import io.github.Romariok.orkestro.models.User;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
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
        User user = new User();
        user.setId(1L);
        user.setUsername("user");
        user.setPassword("password");

        Role role = new Role();
        role.setId(10L);
        role.setName("Leader");
        role.setScope(RoleScopeType.ORGANIZATION);
        role.setOrganizationId(100L);

        Permission permission = new Permission();
        permission.setCode("ORG_EDIT");
        permission.setDescription("Edit organization");

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(userRoleRepository.findRolesByUserId(1L)).thenReturn(List.of(role));
        when(rolePermissionRepository.findPermissionsByRoleId(10L)).thenReturn(List.of(permission));

        UserDetails userDetails = userService.loadUserByUsername("user");
        Set<String> authorityStrings = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(authorityStrings.contains("ROLE_Leader"));
        assertTrue(authorityStrings.contains("CTX_ROLE_ORG:100:Leader"));
        assertTrue(authorityStrings.contains("ORG_EDIT"));
        assertTrue(authorityStrings.contains("CTX_PERM_ORG:100:ORG_EDIT"));

        assertEquals(4, authorityStrings.size());
    }

    @Test
    void loadUserByUsername_withSectionRole_buildsContextualAuthorities() {
        User user = new User();
        user.setId(2L);
        user.setUsername("section-user");
        user.setPassword("password");

        Role role = new Role();
        role.setId(20L);
        role.setName("SectionLeader");
        role.setScope(RoleScopeType.SECTION);
        role.setSectionId(5L);

        Permission permission = new Permission();
        permission.setCode("SECTION_EDIT");
        permission.setDescription("Edit section");

        when(userRepository.findByUsername("section-user")).thenReturn(Optional.of(user));
        when(userRoleRepository.findRolesByUserId(2L)).thenReturn(List.of(role));
        when(rolePermissionRepository.findPermissionsByRoleId(20L)).thenReturn(List.of(permission));

        UserDetails userDetails = userService.loadUserByUsername("section-user");
        Set<String> authorityStrings = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(authorityStrings.contains("ROLE_SectionLeader"));
        assertTrue(authorityStrings.contains("CTX_ROLE_SECTION:5:SectionLeader"));
        assertTrue(authorityStrings.contains("SECTION_EDIT"));
        assertTrue(authorityStrings.contains("CTX_PERM_SECTION:5:SECTION_EDIT"));

        assertEquals(4, authorityStrings.size());
    }
}


