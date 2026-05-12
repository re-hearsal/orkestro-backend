package io.github.Romariok.orkestro.repertoire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentDTO {

   private Long id;
   private String name;
   private String pictureUrl;
}

