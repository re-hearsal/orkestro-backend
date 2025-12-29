package io.github.Romariok.orkestro.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongDTO {

   private Long id;
   private Long organizationId;
   private String title;
   private String composer;
   private Integer durationSeconds;
   private String description;
   private String videoUrl;
   private Instant createdAt;

   private List<SongInstrumentDTO> instrumentation;
   private List<Long> fileIds;
}
