package io.github.Romariok.orkestro.utils.file.dto;

import io.github.Romariok.orkestro.utils.file.FileType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadRequestDTO {

   private MultipartFile file;

   private FileType fileType;

   @Positive
   private Long uploadedByUserId;

   @AssertTrue(message = "File is required")
   private boolean isFilePresent() {
      return file != null && !file.isEmpty() && file.getSize() > 0;
   }

   @AssertTrue(message = "File name is required")
   private boolean isFileNamePresent() {
      if (file == null) {
         return false;
      }
      String originalName = file.getOriginalFilename();
      return originalName != null && !originalName.isBlank();
   }
}
