package io.github.Romariok.orkestro.repertoire.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.repertoire.service.RepertoireService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SongControllerTest {

    @Mock
    private RepertoireService repertoireService;

    @InjectMocks
    private SongController songController;

    private SongDTO buildSong(Long id, String title) {
        return new SongDTO(id, 1L, title, "Composer", 300, null, null,
                Instant.parse("2026-01-01T00:00:00Z"), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void createSong_returnsCreated() {
        SongDTO created = buildSong(1L, "Bohemian Rhapsody");
        when(repertoireService.createSong(eq(1L), any(SongCreateRequestDTO.class))).thenReturn(created);
        SongCreateRequestDTO request = new SongCreateRequestDTO(
                "Bohemian Rhapsody", "Freddie Mercury", 354, null, null,
                List.of(), List.of(), List.of(), List.of());

        var result = songController.createSong(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Bohemian Rhapsody", result.getBody().getTitle());
    }

    @Test
    void updateSong_returnsOk() {
        SongDTO updated = buildSong(1L, "New Title");
        when(repertoireService.updateSong(eq(1L), eq(1L), any(SongUpdateRequestDTO.class))).thenReturn(updated);
        SongUpdateRequestDTO request = new SongUpdateRequestDTO("New Title", null, null, null, null,
                null, null, null, null);

        var result = songController.updateSong(1L, 1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("New Title", result.getBody().getTitle());
    }

    @Test
    void deleteSong_returnsNoContent() {
        doNothing().when(repertoireService).deleteSong(1L, 1L);

        var result = songController.deleteSong(1L, 1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void getSong_returnsOk() {
        SongDTO song = buildSong(1L, "Bohemian Rhapsody");
        when(repertoireService.getSong(1L, 1L)).thenReturn(song);

        var result = songController.getSong(1L, 1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("Bohemian Rhapsody", result.getBody().getTitle());
    }

    @Test
    void searchSongsPage_returnsOk() {
        Page<SongDTO> page = new PageImpl<>(List.of(buildSong(1L, "Song A"), buildSong(2L, "Song B")));
        when(repertoireService.searchSongs(eq(1L), isNull(), any())).thenReturn(page);

        var result = songController.searchSongsPage(1L, null, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTotalElements());
    }

    @Test
    void getSongTags_returnsOk() {
        when(repertoireService.getOrganizationSongTags(1L)).thenReturn(List.of("classic", "rock"));

        var result = songController.getSongTags(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().size());
        assertEquals("classic", result.getBody().get(0));
    }
}
