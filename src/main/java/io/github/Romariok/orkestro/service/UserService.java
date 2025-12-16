package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.models.Permission;
import io.github.Romariok.orkestro.models.Role;
import io.github.Romariok.orkestro.models.User;
import io.github.Romariok.orkestro.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

   private final UserRepository userRepository;
   private final UserRoleRepository userRoleRepository;
   private final RolePermissionRepository rolePermissionRepository;

   @Override
   @Transactional(readOnly = true)
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

      Set<Role> roles = new HashSet<>(userRoleRepository.findRolesByUserId(user.getId()));
      Set<SimpleGrantedAuthority> authorities = new HashSet<>();

      for (Role role : roles) {
         authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

         String scopePrefix = null;
         Long contextId = null;

         if (role.getScope() == RoleScopeType.ORGANIZATION) {
            scopePrefix = "ORG";
            contextId = role.getOrganizationId();
         } else if (role.getScope() == RoleScopeType.SECTION) {
            scopePrefix = "SECTION";
            contextId = role.getSectionId();
         }

         if (scopePrefix != null && contextId != null) {
            authorities.add(new SimpleGrantedAuthority(
                  "CTX_ROLE_" + scopePrefix + ":" + contextId + ":" + role.getName()));
         }

         List<Permission> rolePermissions = rolePermissionRepository.findPermissionsByRoleId(role.getId());
         for (Permission permission : rolePermissions) {
            authorities.add(new SimpleGrantedAuthority(permission.getCode()));

            if (scopePrefix != null && contextId != null) {
               authorities.add(new SimpleGrantedAuthority(
                     "CTX_PERM_" + scopePrefix + ":" + contextId + ":" + permission.getCode()));
            }
         }
      }

      return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            authorities);
   }

   @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
