package io.github.Romariok.orkestro.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongUpdateRequestDTO {

   private String title;
   private String composer;
   private Integer durationSeconds;
   private String description;
   private String videoUrl;

   /**
    * Если не null — полностью заменить инструментальный состав.
    */
   private List<SongInstrumentDTO> instrumentation;

   /**
    * Если не null — полностью заменить список файлов.
    */
   private List<Long> fileIds;
}
