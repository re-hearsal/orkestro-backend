package io.github.Romariok.orkestro.mapper;

import io.github.Romariok.orkestro.dto.SongDTO;
import io.github.Romariok.orkestro.models.Song;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SongMapper {

   SongDTO toDto(Song song);

   List<SongDTO> toDtoList(List<Song> songs);
}
