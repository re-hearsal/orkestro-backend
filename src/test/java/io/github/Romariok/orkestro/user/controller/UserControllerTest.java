package io.github.Romariok.orkestro.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.dto.CurrentUserResponseDTO;
import io.github.Romariok.orkestro.user.dto.MusicalRoleDTO;
import io.github.Romariok.orkestro.user.service.MusicalRoleService;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.user.service.UserTelegramLinkService;
import io.github.Romariok.orkestro.user.service.UserVkLinkService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private MusicalRoleService musicalRoleService;

    @Mock
    private UserTelegramLinkService userTelegramLinkService;

    @Mock
    private UserVkLinkService userVkLinkService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUserProfile_returnsOk() {
        CurrentUserResponseDTO profile = new CurrentUserResponseDTO(1L, "john_doe", "John Doe",
                "john@example.com", "Moscow", null, null, null, null, null);
        when(userService.getCurrentUserProfile()).thenReturn(profile);

        var result = userController.getCurrentUserProfile();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("john_doe", result.getBody().getUsername());
    }

    @Test
    void getMyMusicalRoles_returnsOk() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        when(musicalRoleService.getUserMusicalRoles(1L))
                .thenReturn(List.of(new MusicalRoleDTO(2L, "Violin")));

        var result = userController.getMyMusicalRoles();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Violin", result.getBody().get(0).getInstrumentName());
    }

    @Test
    void createTelegramLinkToken_returnsOk() {
        when(userTelegramLinkService.createLinkTokenForCurrentUser()).thenReturn("tlg_abc123");

        var result = userController.createTelegramLinkToken();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("tlg_abc123", result.getBody().getToken());
    }

    @Test
    void unlinkTelegram_returnsNoContent() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        doNothing().when(userTelegramLinkService).unlinkTelegram(1L);

        var result = userController.unlinkTelegram();

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void createVkLinkToken_returnsOk() {
        when(userVkLinkService.generateLinkTokenForCurrentUser()).thenReturn("vk_token_xyz");

        var result = userController.createVkLinkToken();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("vk_token_xyz", result.getBody().getToken());
    }

    @Test
    void unlinkVk_returnsNoContent() {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);
        doNothing().when(userVkLinkService).unlinkVk(1L);

        var result = userController.unlinkVk();

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
