package io.github.Romariok.orkestro.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.user.dto.PublicUserProfileDTO;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.service.UserService;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PublicUserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicUserController publicUserController;

    @Test
    void getPublicUserProfile_userFound_returnsOkWithDTO() {
        Long userId = 1L;
        PublicUserProfileDTO dto = new PublicUserProfileDTO(
                userId, "john_doe", "John Doe", "john@example.com",
                "Moscow", LocalDate.of(1990, 5, 15), UserLanguageType.RU, 42L, null);
        when(userService.getPublicUserProfile(userId)).thenReturn(dto);

        var result = publicUserController.getPublicUserProfile(userId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userId, result.getBody().getId());
        assertEquals("john_doe", result.getBody().getUsername());
        assertEquals("John Doe", result.getBody().getName());
    }

    @Test
    void getPublicUserProfile_userNotFound_propagatesException() {
        Long userId = 999L;
        when(userService.getPublicUserProfile(userId))
                .thenThrow(new EntityNotFoundException("User not found: " + userId));

        org.junit.jupiter.api.Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> publicUserController.getPublicUserProfile(userId));
    }
}
