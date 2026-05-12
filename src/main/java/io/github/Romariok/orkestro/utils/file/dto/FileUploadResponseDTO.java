package io.github.Romariok.orkestro.utils.file.dto;

import io.github.Romariok.orkestro.utils.file.FileType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileUploadResponseDTO {
   private Long id;
   private String name;
   private FileType fileType;
   private Long size;
}
