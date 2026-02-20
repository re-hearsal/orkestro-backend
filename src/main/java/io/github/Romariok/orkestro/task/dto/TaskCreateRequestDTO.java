package io.github.Romariok.orkestro.task.dto;

import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskCreateRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Positive
    private Long assigneeUserId;

    @NotNull
    private TaskVisibility visibility;
    @Size(max = 50)
    private List<@NotNull @Positive Long> visibilityRoleIds;

    /**
     * Файлы, прикрепляемые к задаче при создании.
     */
    @Size(max = 50)
    private List<MultipartFile> files;

    @AssertTrue(message = "Files must be non-empty with valid names")
    private boolean isFilesValid() {
        if (files == null || files.isEmpty()) {
            return true;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() <= 0) {
                return false;
            }
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
