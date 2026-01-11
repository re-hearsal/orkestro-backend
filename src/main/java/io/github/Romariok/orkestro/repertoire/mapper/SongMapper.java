package io.github.Romariok.orkestro.repertoire.mapper;

import java.util.List;
import org.mapstruct.Mapper;

import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.models.Song;

@Mapper(componentModel = "spring")
public interface SongMapper {

   SongDTO toDto(Song song);

   List<SongDTO> toDtoList(List<Song> songs);
}
