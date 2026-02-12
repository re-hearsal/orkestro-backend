package io.github.Romariok.orkestro.task.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskFileAttachRequestDTO {

    @NotNull
    private MultipartFile file;

    @AssertTrue(message = "File must be non-empty with valid name")
    private boolean isFileValid() {
        if (file == null) {
            return false;
        }
        if (file.isEmpty() || file.getSize() <= 0) {
            return false;
        }
        String originalName = file.getOriginalFilename();
        return originalName != null && !originalName.isBlank();
    }
}
