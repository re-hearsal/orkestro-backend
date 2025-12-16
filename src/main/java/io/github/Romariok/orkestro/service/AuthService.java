package io.github.Romariok.orkestro.service;
import io.github.Romariok.orkestro.security.JWTUtil;
import io.github.Romariok.orkestro.dto.AuthResponseDTO;
import io.github.Romariok.orkestro.dto.RegisterRequestDTO;
import io.github.Romariok.orkestro.dto.LoginRequestDTO;
import io.github.Romariok.orkestro.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userService.existsByUsername(request.getUsername())) {
            log.warn("Username already taken: {}", request.getUsername());
            throw new RuntimeException("Username is already taken");
        }

        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userService.saveUser(user);
            log.info("User saved successfully: {}", user.getUsername());
            String token = generateTokenForUser(request.getUsername());
            log.info("Token generated for new user: {}", user.getUsername());
            return new AuthResponseDTO(token, user.getUsername());
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw e;
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
        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            throw e;
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
            throw e;
        }
    }
}