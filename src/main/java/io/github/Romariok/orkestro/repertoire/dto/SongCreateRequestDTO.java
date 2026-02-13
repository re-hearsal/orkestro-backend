package io.github.Romariok.orkestro.repertoire.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongCreateRequestDTO {

   @NotBlank
   @Size(max = 255)
   private String title;

   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Composer cannot be blank")
   private String composer;

   @Positive
   private Integer durationSeconds;

   @Size(max = 2000)
   @Pattern(regexp = ".*\\S.*", message = "Description cannot be blank")
   private String description;

   @Size(max = 2048)
   @Pattern(regexp = ".*\\S.*", message = "Video URL cannot be blank")
   private String videoUrl;

   @NotNull
   @Size(min = 1)
   private List<@Valid @NotNull SongInstrumentDTO> instrumentation;

   private List<@NotNull @NotBlank @Size(max = 50) String> tags;

   /**
    * Загружаемые файлы с нотами (PDF/PHOTO).
    */
  @Size(max = 50)
   private List<MultipartFile> sheetFiles;

   /**
    * Загружаемые аудио файлы (AUDIO).
    */
  @Size(max = 50)
   private List<MultipartFile> audioFiles;
}
