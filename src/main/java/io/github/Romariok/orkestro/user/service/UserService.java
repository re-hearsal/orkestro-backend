package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.dto.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;

import java.time.Instant;
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
   private final StoredFileRepository storedFileRepository;
   private final UserInstrumentRepository userInstrumentRepository;
   private final SecurityUtils securityUtils;

   @Override
   @Transactional(readOnly = true)
   public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
      User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

      Set<Role> roles = new HashSet<>(userRoleRepository.findRolesByUserId(user.getId()));
      Set<SimpleGrantedAuthority> authorities = new HashSet<>();

      for (Role role : roles) {
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

   @Transactional
   public User updateUserProfile(Long userId, UserProfileUpdateRequestDTO request) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      if (request.getName() != null) {
         user.setName(request.getName());
      }
      if (request.getEmail() != null) {
         user.setEmail(request.getEmail());
      }
      if (request.getLocation() != null) {
         user.setLocation(request.getLocation());
      }
      if (request.getBirthDate() != null) {
         user.setBirthDate(request.getBirthDate());
      }
      if (request.getNotificationChannel() != null) {
         NotificationChannelType channel = request.getNotificationChannel();
         user.setNotificationChannel(channel);
         if (channel == NotificationChannelType.EMAIL) {
            user.setTelegramUserId(null);
         }
      }

      user.setUpdatedAt(Instant.now());
      return userRepository.save(user);
   }

   @Transactional
   public User updateCurrentUserProfile(UserProfileUpdateRequestDTO request) {
      Long currentUserId = securityUtils.getCurrentUserId();
      return updateUserProfile(currentUserId, request);
   }

   @Transactional
   public void deleteUserAccount(Long userId) {
      if (!userRepository.existsById(userId)) {
         throw new EntityNotFoundException("User not found: " + userId);
      }

      userInstrumentRepository.deleteByUserId(userId);
      userRoleRepository.deleteByUserId(userId);

      userRepository.deleteById(userId);
   }

   @Transactional
   public void deleteCurrentUserAccount() {
      Long currentUserId = securityUtils.getCurrentUserId();
      deleteUserAccount(currentUserId);
   }

   @Transactional(readOnly = true)
   public List<User> searchUsers(String nameQuery, List<Long> roleIds) {
      String normalizedName = (nameQuery == null || nameQuery.isBlank())
            ? null
            : nameQuery.trim();

      boolean filterByRoles = roleIds != null && !roleIds.isEmpty();

      if (!filterByRoles) {
         if (normalizedName == null) {
            return userRepository.findAll();
         }
         return userRepository.findByNameContainingIgnoreCase(normalizedName);
      }

      return userRepository.findByNameAndRoleIds(normalizedName, roleIds);
   }

   @Transactional
   public User updateNotificationChannel(Long userId, NotificationChannelType channel) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      user.setNotificationChannel(channel);
      if (channel == NotificationChannelType.EMAIL) {
         user.setTelegramUserId(null);
      }

      user.setUpdatedAt(Instant.now());
      return userRepository.save(user);
   }

   @Transactional
   public User updateCurrentUserNotificationChannel(NotificationChannelType channel) {
      Long currentUserId = securityUtils.getCurrentUserId();
      return updateNotificationChannel(currentUserId, channel);
   }

   @Transactional
   public void updateCurrentUserProfileImage(Long fileId) {
      Long currentUserId = securityUtils.getCurrentUserId();
      updateProfileImage(currentUserId, fileId);
   }

   @Transactional
   public void updateProfileImage(Long userId, Long fileId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      StoredFile file = storedFileRepository.findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));

      user.setProfileImageFileId(file.getId());
      userRepository.save(user);
   }
}
