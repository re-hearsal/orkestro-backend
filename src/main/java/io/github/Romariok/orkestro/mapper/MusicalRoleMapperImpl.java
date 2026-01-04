package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.role.MusicalRoleDTO;
import io.github.Romariok.orkestro.models.role.Instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Ручная реализация {@link MusicalRoleMapper}, эквивалентная тому, что
 * генерирует MapStruct.
 */
@Component
public class MusicalRoleMapperImpl implements MusicalRoleMapper {

   @Override
   public MusicalRoleDTO toDto(Instrument instrument) {
      if (instrument == null) {
         return null;
      }

      MusicalRoleDTO dto = new MusicalRoleDTO();
      dto.setInstrumentId(instrument.getId());
      dto.setInstrumentName(instrument.getName());
      return dto;
   }

   @Override
   public List<MusicalRoleDTO> toDtoList(List<Instrument> instruments) {
      if (instruments == null || instruments.isEmpty()) {
         return List.of();
      }

      List<MusicalRoleDTO> list = new ArrayList<>(instruments.size());
      for (Instrument instrument : instruments) {
         if (Objects.nonNull(instrument)) {
            list.add(toDto(instrument));
         }
      }
      return list;
   }
}
