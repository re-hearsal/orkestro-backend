package io.github.Romariok.orkestro.repertoire.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongFileUploadRequestDTO {

   private MultipartFile file;

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

