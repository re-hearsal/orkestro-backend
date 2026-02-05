package io.github.Romariok.orkestro.repertoire.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongInstrumentDTO {

   @NotNull
   @Positive
   private Long instrumentId;

   @NotNull
   @Min(1)
   private Integer count;
}
