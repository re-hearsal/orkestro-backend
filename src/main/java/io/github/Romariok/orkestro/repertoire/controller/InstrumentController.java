package io.github.Romariok.orkestro.repertoire.controller;

import io.github.Romariok.orkestro.repertoire.dto.InstrumentDTO;
import io.github.Romariok.orkestro.repertoire.repository.InstrumentRepository;
import io.github.Romariok.orkestro.user.models.Instrument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/instruments")
public class InstrumentController {

   private final InstrumentRepository instrumentRepository;

   @Value("${orkestro.instrument.icon-url-prefix:/img/instruments/}")
   private String iconUrlPrefix;

   @Value("${orkestro.instrument.icon-url-suffix:.svg}")
   private String iconUrlSuffix;

   @GetMapping
   public ResponseEntity<List<InstrumentDTO>> getAllInstruments() {
      List<Instrument> instruments = instrumentRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
      List<InstrumentDTO> dtos = instruments.stream()
            .map(this::toDto)
            .toList();
      return ResponseEntity.ok(dtos);
   }

   private InstrumentDTO toDto(Instrument instrument) {
      String pictureUrl = instrument.getIconKey() != null
            ? (iconUrlPrefix + instrument.getIconKey() + iconUrlSuffix)
            : null;
      return new InstrumentDTO(instrument.getId(), instrument.getName(), pictureUrl);
   }
}

