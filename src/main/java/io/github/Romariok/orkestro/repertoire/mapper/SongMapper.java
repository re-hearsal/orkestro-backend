package io.github.Romariok.orkestro.repertoire.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.models.Song;

@Mapper(componentModel = "spring")
public interface SongMapper {

   @Mapping(target = "instrumentation", ignore = true)
   @Mapping(target = "sheetFileIds", ignore = true)
   @Mapping(target = "audioFileIds", ignore = true)
   SongDTO toDto(Song song);

   List<SongDTO> toDtoList(List<Song> songs);
}
