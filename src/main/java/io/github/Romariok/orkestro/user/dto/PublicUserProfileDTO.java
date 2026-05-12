package io.github.Romariok.orkestro.user.dto;

import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserProfileDTO {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String location;
    private LocalDate birthDate;
    private UserLanguageType preferredLanguage;
    private Long profileImageFileId;
    private List<MusicalRoleDTO> instruments;
}
