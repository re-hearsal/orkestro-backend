package io.github.Romariok.orkestro.repertoire.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongDTO {

   @Schema(description = "Идентификатор песни", example = "1")
   private Long id;
   @Schema(description = "Идентификатор организации", example = "1")
   private Long organizationId;
   @Schema(description = "Название произведения", example = "Bohemian Rhapsody")
   private String title;
   @Schema(description = "Композитор", example = "Freddie Mercury")
   private String composer;
   @Schema(description = "Длительность в секундах", example = "354")
   private Integer durationSeconds;
   @Schema(description = "Описание произведения", maxLength = 3000, example = "A six-minute suite with distinct sections.")
   private String description;
   @Schema(description = "Ссылка на видео", example = "https://example.com/video")
   private String videoUrl;
   private Instant createdAt;

   private List<SongInstrumentDTO> instrumentation;
   private List<String> tags;
   private List<Long> sheetFileIds;
   private List<Long> audioFileIds;
   private List<Long> fileIds;
}
