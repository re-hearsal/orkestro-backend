package io.github.Romariok.orkestro.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongInstrumentDTO {

   @NotNull
   private Long instrumentId;

   @NotNull
   @Min(1)
   private Integer count;
}
