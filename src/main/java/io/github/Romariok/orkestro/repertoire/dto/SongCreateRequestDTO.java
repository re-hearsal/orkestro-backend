package io.github.Romariok.orkestro.repertoire.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongCreateRequestDTO {

   @NotBlank
   private String title;

   private String composer;
   private Integer durationSeconds;
   private String description;
   private String videoUrl;

   private List<SongInstrumentDTO> instrumentation;
   private List<Long> fileIds;
}
