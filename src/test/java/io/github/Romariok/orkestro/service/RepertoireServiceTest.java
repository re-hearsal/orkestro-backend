package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongInstrumentDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.repertoire.mapper.SongMapper;
import io.github.Romariok.orkestro.repertoire.models.Song;
import io.github.Romariok.orkestro.repertoire.models.SongFile;
import io.github.Romariok.orkestro.repertoire.models.SongInstrument;
import io.github.Romariok.orkestro.repertoire.repository.InstrumentRepository;
import io.github.Romariok.orkestro.repertoire.repository.SongFileRepository;
import io.github.Romariok.orkestro.repertoire.repository.SongInstrumentRepository;
import io.github.Romariok.orkestro.repertoire.repository.SongRepository;
import io.github.Romariok.orkestro.repertoire.service.RepertoireService;
import io.github.Romariok.orkestro.user.models.Instrument;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RepertoireServiceTest {

   @Mock
   private SongRepository songRepository;

   @Mock
   private SongInstrumentRepository songInstrumentRepository;

   @Mock
   private SongFileRepository songFileRepository;

   @Mock
   private InstrumentRepository instrumentRepository;

   @Mock
   private StoredFileRepository storedFileRepository;

   @Mock
   private SongMapper songMapper;

   @InjectMocks
   private RepertoireService repertoireService;

   @Test
   void createSong_success_savesSongInstrumentationAndFiles() {
      Long orgId = 1L;

      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title",
            "Composer",
            120,
            "Desc",
            "videoUrl",
            List.of(new SongInstrumentDTO(10L, 2)),
            List.of(100L, 101L));

      Instrument instrument = Instrument.builder()
            .id(10L)
            .name("Violin")
            .build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      StoredFile f1 = StoredFile.builder().id(100L).build();
      StoredFile f2 = StoredFile.builder().id(101L).build();
      when(storedFileRepository.findAllById(any())).thenReturn(List.of(f1, f2));

      Song savedSong = Song.builder()
            .id(5L)
            .organizationId(orgId)
            .title("Title")
            .createdAt(Instant.now())
            .build();
      when(songRepository.save(any(Song.class))).thenReturn(savedSong);

      SongDTO mapped = new SongDTO();
      mapped.setId(5L);
      mapped.setOrganizationId(orgId);
      when(songMapper.toDto(savedSong)).thenReturn(mapped);

      SongDTO result = repertoireService.createSong(orgId, request);

      assertEquals(5L, result.getId());
      assertEquals(orgId, result.getOrganizationId());

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<SongInstrument>> instrCaptor = ArgumentCaptor
            .forClass((Class<List<SongInstrument>>) (Class<?>) List.class);
      verify(songInstrumentRepository).saveAll(instrCaptor.capture());
      List<SongInstrument> storedInstruments = instrCaptor.getValue();
      assertEquals(1, storedInstruments.size());
      assertEquals(10L, storedInstruments.getFirst().getInstrumentId());
      assertEquals(2, storedInstruments.getFirst().getCount());

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<SongFile>> fileCaptor = ArgumentCaptor
            .forClass((Class<List<SongFile>>) (Class<?>) List.class);
      verify(songFileRepository).saveAll(fileCaptor.capture());
      List<SongFile> storedFiles = fileCaptor.getValue();
      assertEquals(2, storedFiles.size());
   }

   @Test
   void createSong_nullInstrumentation_throwsIllegalArgument() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null, null, null);

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void createSong_emptyInstrumentation_throwsIllegalArgument() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null, List.of(), null);

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void createSong_instrumentNotFound_throwsEntityNotFound() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null, List.of(new SongInstrumentDTO(10L, 1)), null);

      when(instrumentRepository.findAllById(any()))
            .thenReturn(List.of()); // ничего не найдено

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void createSong_fileNotFound_throwsEntityNotFound() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title",
            null,
            null,
            null,
            null,
            List.of(new SongInstrumentDTO(10L, 1)),
            List.of(100L));

      Instrument instrument = Instrument.builder()
            .id(10L)
            .name("Violin")
            .build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      when(storedFileRepository.findAllById(any()))
            .thenReturn(List.of()); // файл не найден

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void updateSong_notFound_throwsEntityNotFound() {
      when(songRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.updateSong(1L, new SongUpdateRequestDTO()));
   }

   @Test
   void updateSong_updatesBasicFields_only() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Old")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));

      when(songMapper.toDto(existing)).thenReturn(new SongDTO());
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setTitle("New");
      request.setDescription("Desc");

      repertoireService.updateSong(1L, request);

      verify(songInstrumentRepository, never()).deleteBySongId(1L);
      verify(songFileRepository, never()).deleteBySongId(1L);
      assertEquals("New", existing.getTitle());
      assertEquals("Desc", existing.getDescription());
   }

   @Test
   void updateSong_withInstrumentation_replacesInstrumentation() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      Instrument instrument = Instrument.builder()
            .id(10L)
            .name("Violin")
            .build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      when(songMapper.toDto(existing)).thenReturn(new SongDTO());
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setInstrumentation(List.of(new SongInstrumentDTO(10L, 2)));

      repertoireService.updateSong(1L, request);

      verify(songInstrumentRepository).deleteBySongId(1L);
      verify(songInstrumentRepository).saveAll(any());
   }

   @Test
   void updateSong_withEmptyInstrumentation_throwsIllegalArgument() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setInstrumentation(List.of());

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.updateSong(1L, request));
   }

   @Test
   void updateSong_withFiles_clearsAndRecreatesFiles() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      StoredFile f1 = StoredFile.builder().id(100L).build();
      when(storedFileRepository.findAllById(any())).thenReturn(List.of(f1));

      when(songMapper.toDto(existing)).thenReturn(new SongDTO());
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of(100L));

      repertoireService.updateSong(1L, request);

      verify(songFileRepository).deleteBySongId(1L);
      verify(songFileRepository).saveAll(any());
   }

   @Test
   void updateSong_withEmptyFileList_clearsFiles() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      when(songMapper.toDto(existing)).thenReturn(new SongDTO());
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of());

      repertoireService.updateSong(1L, request);

      verify(songFileRepository).deleteBySongId(1L);
      // saveSongFiles не должен ничего сохранить при пустом списке
      verify(songFileRepository, never()).saveAll(any());
   }

   @Test
   void updateSong_filesNotFound_throwsEntityNotFound() {
      Song existing = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      when(storedFileRepository.findAllById(any()))
            .thenReturn(List.of()); // ничего не найдено

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of(100L));

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.updateSong(1L, request));
   }

   @Test
   void deleteSong_notFound_throwsEntityNotFound() {
      when(songRepository.existsById(1L)).thenReturn(false);

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.deleteSong(1L));
   }

   @Test
   void deleteSong_existing_deletes() {
      when(songRepository.existsById(1L)).thenReturn(true);

      repertoireService.deleteSong(1L);

      verify(songRepository).deleteById(1L);
   }

   @Test
   void getSong_notFound_throwsEntityNotFound() {
      when(songRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.getSong(1L));
   }

   @Test
   void getSong_returnsDtoWithInstrumentationAndFiles() {
      Song song = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("Song")
            .build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(song));

      SongDTO baseDto = new SongDTO();
      baseDto.setId(1L);
      baseDto.setOrganizationId(1L);
      when(songMapper.toDto(song)).thenReturn(baseDto);

      SongInstrument si = new SongInstrument();
      si.setSongId(1L);
      si.setInstrumentId(10L);
      si.setCount(2);
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of(si));

      SongFile sf = new SongFile();
      sf.setSongId(1L);
      sf.setFileId(100L);
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of(sf));

      SongDTO dto = repertoireService.getSong(1L);

      assertEquals(1, dto.getInstrumentation().size());
      assertEquals(10L, dto.getInstrumentation().getFirst().getInstrumentId());
      assertEquals(2, dto.getInstrumentation().getFirst().getCount());

      assertEquals(1, dto.getFileIds().size());
      assertEquals(100L, dto.getFileIds().getFirst());
   }

   @Test
   void getSongsByOrganization_returnsPagedDtosWithInstrumentationAndFiles() {
      Song song1 = Song.builder()
            .id(1L)
            .organizationId(1L)
            .title("S1")
            .build();
      Song song2 = Song.builder()
            .id(2L)
            .organizationId(1L)
            .title("S2")
            .build();

      Page<Song> page = new PageImpl<>(List.of(song1, song2), PageRequest.of(0, 10), 2);
      when(songRepository.findByOrganizationId(1L,
            PageRequest.of(0, 10,
                  org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                        "createdAt"))))
            .thenReturn(page);

      SongDTO dto1 = new SongDTO();
      dto1.setId(1L);
      SongDTO dto2 = new SongDTO();
      dto2.setId(2L);
      when(songMapper.toDto(song1)).thenReturn(dto1);
      when(songMapper.toDto(song2)).thenReturn(dto2);

      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songInstrumentRepository.findBySongId(2L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(2L)).thenReturn(List.of());

      Page<SongDTO> result = repertoireService.getSongsByOrganization(1L, 0, 10);

      assertEquals(2, result.getTotalElements());
      assertEquals(2, result.getContent().size());
      assertEquals(1L, result.getContent().getFirst().getId());
      assertEquals(2L, result.getContent().get(1).getId());
   }
}
