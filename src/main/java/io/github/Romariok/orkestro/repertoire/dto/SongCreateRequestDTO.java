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
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongCreateRequestDTO {

   @NotBlank
   @Size(max = 255)
   @Schema(description = "Название произведения", maxLength = 255, example = "Bohemian Rhapsody")
   private String title;

   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Composer cannot be blank")
   @Schema(description = "Композитор", maxLength = 255, example = "Freddie Mercury")
   private String composer;

   @Positive
   @Schema(description = "Длительность в секундах", example = "354")
   private Integer durationSeconds;

   @Size(max = 3000)
   @Pattern(regexp = ".*\\S.*", message = "Description cannot be blank")
   @Schema(description = "Описание произведения", maxLength = 3000, example = "A six-minute suite with distinct sections.")
   private String description;

   @Size(max = 2048)
   @Pattern(regexp = ".*\\S.*", message = "Video URL cannot be blank")
   @Schema(description = "Ссылка на видео", maxLength = 2048, example = "https://example.com/video")
   private String videoUrl;

   @NotNull
   @Size(min = 1)
   private List<@Valid @NotNull SongInstrumentDTO> instrumentation;

   private List<@NotNull @NotBlank @Size(max = 50) String> tags;

   @Size(max = 50)
   private List<MultipartFile> files;
}
