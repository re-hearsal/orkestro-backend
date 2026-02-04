package io.github.Romariok.orkestro.user.service;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.security.JWTUtil;
import io.github.Romariok.orkestro.user.dto.AuthResponseDTO;
import io.github.Romariok.orkestro.user.dto.LoginRequestDTO;
import io.github.Romariok.orkestro.user.dto.RegisterRequestDTO;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.exception.InternalServiceException;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final FileStorageService fileStorageService;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String name = request.getName() == null ? null : request.getName().trim();
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String location = request.getLocation() == null ? null : request.getLocation().trim();

        log.info("Registering new user: {}", username);

        if (userService.existsByUsername(username)) {
            log.warn("Username already taken: {}", username);
            throw new BusinessException("Username is already taken");
        }

        try {
            Instant now = Instant.now();
            User user = User.builder()
                    .username(username)
                    .name(name)
                    .email(email)
                    .location(location)
                    .birthDate(request.getBirthDate())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .createdAt(now)
                    .updatedAt(now)
                    .notificationChannel(NotificationChannelType.EMAIL)
                    .build();
            userService.saveUser(user);

            if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
                StoredFile avatar = fileStorageService.upload(
                        request.getAvatar(),
                        FileType.PHOTO,
                        user.getId());
                user.setProfileImageFileId(avatar.getId());
                user.setUpdatedAt(Instant.now());
                userService.saveUser(user);
            }

            log.info("User saved successfully: {}", user.getUsername());
            String token = generateTokenForUser(username);
            log.info("Token generated for new user: {}", user.getUsername());
            return new AuthResponseDTO(token, user.getUsername());
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw new InternalServiceException("Error during user registration", e);
        }
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Processing login for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("User authenticated successfully: {}", request.getUsername());
            String token = generateTokenForUser(request.getUsername());
            log.info("Token generated for user: {}", request.getUsername());

            return new AuthResponseDTO(token, request.getUsername());
        } catch (BadCredentialsException e) {
            log.error("Authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", request.getUsername(), e.getMessage(), e);
            throw new InternalServiceException("Error during user login", e);
        }
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BusinessException("No authenticated user found");
        }

        String username = authentication.getName();
        changePasswordForUser(username, currentPassword, newPassword);
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        log.info("Resetting password for user: {}", username);

        try {
            User user = userService.findByUsername(username);
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.saveUser(user);
        } catch (UsernameNotFoundException e) {
            throw new EntityNotFoundException("User not found: " + username);
        } catch (Exception e) {
            log.error("Error resetting password for user {}: {}", username, e.getMessage(), e);
            throw new InternalServiceException("Error resetting password for user: " + username, e);
        }
    }

    private String generateTokenForUser(String username) {
        log.debug("Generating token for user: {}", username);
        try {
            UserDetails userDetails = userService.loadUserByUsername(username);
            Set<String> authorities = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            log.debug("User {} has authorities: {}", username, authorities);
            return jwtUtil.generateToken(username, authorities);
        } catch (Exception e) {
            log.error("Error generating token for user {}: {}", username, e.getMessage(), e);
            throw new InternalServiceException("Error generating token for user: " + username, e);
        }
    }

    private void changePasswordForUser(String username, String currentPassword, String newPassword) {
        log.info("Changing password for user: {}", username);

        try {
            User user = userService.findByUsername(username);

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new BusinessException("Current password is incorrect");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userService.saveUser(user);
        } catch (UsernameNotFoundException e) {
            throw new EntityNotFoundException("User not found: " + username);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error changing password for user {}: {}", username, e.getMessage(), e);
            throw new InternalServiceException("Error changing password for user: " + username, e);
        }
    }
}