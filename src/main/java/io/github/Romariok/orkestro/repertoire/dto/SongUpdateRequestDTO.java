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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongUpdateRequestDTO {

   @Size(max = 255)
   @Pattern(regexp = ".*\\S.*", message = "Title cannot be blank")
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

   /**
    * Если не null — полностью заменить инструментальный состав.
    */
   @Size(min = 1)
   private List<@Valid @NotNull SongInstrumentDTO> instrumentation;

   /**
    * Если не null — полностью заменить тэги.
    */
   private List<@NotNull @NotBlank @Size(max = 50) String> tags;

   /**
    * Если не null — полностью заменить список файлов с нотами (PDF/PHOTO).
    * Пустой список означает очистку.
    */
   @Size(max = 50)
   private List<@NotNull @Positive Long> sheetFileIds;

   /**
    * Если не null — полностью заменить список аудио файлов (AUDIO).
    * Пустой список означает очистку.
    */
   @Size(max = 50)
   private List<@NotNull @Positive Long> audioFileIds;
}
