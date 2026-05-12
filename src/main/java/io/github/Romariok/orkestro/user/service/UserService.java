package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.dto.CurrentUserResponseDTO;
import io.github.Romariok.orkestro.user.dto.PublicUserProfileDTO;
import io.github.Romariok.orkestro.user.dto.UserProfileUpdateRequestDTO;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.mapper.CurrentUserMapper;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.UserInstrumentRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

   private final UserRepository userRepository;
   private final UserRoleRepository userRoleRepository;
   private final RolePermissionRepository rolePermissionRepository;
   private final StoredFileRepository storedFileRepository;
   private final UserInstrumentRepository userInstrumentRepository;
   private final FileStorageService fileStorageService;
   private final FileReferenceService fileReferenceService;
   private final CurrentUserMapper currentUserMapper;
   private final SecurityUtils securityUtils;
   private final SimpMessagingTemplate messagingTemplate;

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

   @Transactional(readOnly = true)
   public PublicUserProfileDTO getPublicUserProfile(Long userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
      return new PublicUserProfileDTO(
            user.getId(), user.getUsername(), user.getName(), user.getEmail(),
            user.getLocation(), user.getBirthDate(), user.getPreferredLanguage(),
            user.getProfileImageFileId(), null);
   }

   @Transactional(readOnly = true)
   public CurrentUserResponseDTO getCurrentUserProfile() {
      Long currentUserId = securityUtils.getCurrentUserId();
      User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + currentUserId));
      return currentUserMapper.toDto(user);
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
         user.setName(request.getName().trim());
      }
      if (request.getEmail() != null) {
         user.setEmail(request.getEmail().trim());
      }
      if (request.getLocation() != null) {
         user.setLocation(request.getLocation().trim());
      }
      if (request.getBirthDate() != null) {
         user.setBirthDate(request.getBirthDate());
      }
      if (request.getPreferredLanguage() != null) {
         user.setPreferredLanguage(request.getPreferredLanguage());
      }

      user.setUpdatedAt(Instant.now());
      return userRepository.save(user);
   }

   @Transactional
   public User updateCurrentUserProfile(UserProfileUpdateRequestDTO request) {
      Long currentUserId = securityUtils.getCurrentUserId();
      User updated = updateUserProfile(currentUserId, request);
      CurrentUserResponseDTO profileDTO = currentUserMapper.toDto(updated);
      messagingTemplate.convertAndSendToUser(
            String.valueOf(currentUserId),
            "/queue/profile-updated",
            profileDTO
      );
      return updated;
   }

   @Transactional
   public void deleteUserAccount(Long userId) {
      if (!userRepository.existsById(userId)) {
         throw new EntityNotFoundException("User not found: " + userId);
      }

      storedFileRepository.clearUploadedByUserId(userId);

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
   public void updateCurrentUserProfileImage(MultipartFile file) {
      Long currentUserId = securityUtils.getCurrentUserId();
      updateProfileImage(currentUserId, file);
   }

   @Transactional
   public void deleteCurrentUserProfileImage() {
      Long currentUserId = securityUtils.getCurrentUserId();
      deleteProfileImage(currentUserId);
   }

   @Transactional
   public void updateProfileImage(Long userId, MultipartFile file) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      validateProfileImageFile(file);
      Long previousProfileImageFileId = user.getProfileImageFileId();
      StoredFile uploadedFile = fileStorageService.upload(file, FileType.PHOTO, userId);

      user.setProfileImageFileId(uploadedFile.getId());
      userRepository.save(user);
      if (previousProfileImageFileId != null && !fileReferenceService.isFileReferenced(previousProfileImageFileId)) {
         fileStorageService.delete(previousProfileImageFileId);
      }
   }

   @Transactional
   public void deleteProfileImage(Long userId) {
      User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

      Long profileImageFileId = user.getProfileImageFileId();
      if (profileImageFileId == null) {
         return;
      }

      user.setProfileImageFileId(null);
      userRepository.save(user);
      if (!fileReferenceService.isFileReferenced(profileImageFileId)) {
         fileStorageService.delete(profileImageFileId);
      }
   }

   private void validateProfileImageFile(MultipartFile file) {
      if (file == null || file.isEmpty() || file.getSize() <= 0) {
         throw new BusinessException("Profile image must be a non-empty image file");
      }
      String contentType = file.getContentType();
      if (contentType == null || contentType.isBlank() || !contentType.startsWith("image/")) {
         throw new BusinessException("Profile image must be an image file");
      }
   }
}
