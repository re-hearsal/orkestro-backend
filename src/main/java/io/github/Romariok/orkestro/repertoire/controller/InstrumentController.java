package io.github.Romariok.orkestro.repertoire.controller;

import io.github.Romariok.orkestro.repertoire.dto.InstrumentDTO;
import io.github.Romariok.orkestro.repertoire.repository.InstrumentRepository;
import io.github.Romariok.orkestro.user.models.Instrument;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/instruments")
@Tag(name = "Instruments", description = "API для получения списка музыкальных инструментов")
public class InstrumentController {

   private final InstrumentRepository instrumentRepository;

   @Value("${orkestro.instrument.icon-url-prefix:/img/instruments/}")
   private String iconUrlPrefix;

   @Value("${orkestro.instrument.icon-url-suffix:.svg}")
   private String iconUrlSuffix;

   @Operation(
           summary = "Получить все инструменты",
           description = "Возвращает отсортированный по имени список всех доступных музыкальных инструментов."
   )
   @ApiResponses({
           @ApiResponse(responseCode = "200", description = "Список инструментов получен", content = @Content(schema = @Schema(implementation = List.class))),
           @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
   })
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

