package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
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
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class RepertoireServiceTest {

   @Mock
   private OrganizationRepository organizationRepository;

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
   private FileStorageService fileStorageService;

   @Mock
   private SongMapper songMapper;

   @Mock
   private FileRollbackHelper fileRollbackHelper;

   @Mock
   private FileLimitsProperties fileLimitsProperties;

   @Mock
   private SimpMessagingTemplate messagingTemplate;

   @InjectMocks
   private RepertoireService repertoireService;

   @BeforeEach
   void setup() {
      when(organizationRepository.existsById(anyLong())).thenReturn(true);
      lenient().when(fileLimitsProperties.getSongMaxFiles()).thenReturn(50);
   }

   @Test
   void createSong_success_savesSongInstrumentationAndFiles() {
      Long orgId = 1L;

      MultipartFile sheet = new MockMultipartFile(
            "files", "sheet.jpg", "image/jpeg", "x".getBytes());
      MultipartFile audio = new MockMultipartFile(
            "files", "audio.mp3", "audio/mpeg", "x".getBytes());

      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", "Composer", 120, "Desc", "videoUrl",
            List.of(new SongInstrumentDTO(10L, 2)),
            List.of("tagB", "tagA"),
            List.of(sheet, audio));

      Instrument instrument = Instrument.builder().id(10L).name("Violin").build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      Map<Long, StoredFile> fileMap = new HashMap<>();
      fileMap.put(100L, StoredFile.builder().id(100L).fileType(FileType.PHOTO).build());
      fileMap.put(101L, StoredFile.builder().id(101L).fileType(FileType.AUDIO).build());

      when(fileStorageService.uploadForCurrentUser(sheet, FileType.PHOTO)).thenReturn(fileMap.get(100L));
      when(fileStorageService.uploadForCurrentUser(audio, FileType.AUDIO)).thenReturn(fileMap.get(101L));

      when(storedFileRepository.findAllById(any())).thenAnswer(invocation -> {
         Iterable<Long> ids = invocation.getArgument(0);
         List<StoredFile> out = new ArrayList<>();
         for (Long id : ids) {
            StoredFile f = fileMap.get(id);
            if (f != null) out.add(f);
         }
         return out;
      });

      when(songRepository.save(any(Song.class))).thenAnswer(invocation -> {
         Song s = invocation.getArgument(0);
         s.setId(5L);
         return s;
      });

      when(songMapper.toDto(any(Song.class))).thenAnswer(invocation -> {
         Song s = invocation.getArgument(0);
         SongDTO dto = new SongDTO();
         dto.setId(s.getId());
         dto.setOrganizationId(s.getOrganizationId());
         dto.setTitle(s.getTitle());
         return dto;
      });

      SongInstrument si = new SongInstrument();
      si.setSongId(5L);
      si.setInstrumentId(10L);
      si.setCount(2);
      when(songInstrumentRepository.findBySongId(5L)).thenReturn(List.of(si));

      SongFile sf1 = new SongFile();
      sf1.setSongId(5L);
      sf1.setFileId(100L);
      SongFile sf2 = new SongFile();
      sf2.setSongId(5L);
      sf2.setFileId(101L);
      when(songFileRepository.findBySongId(5L)).thenReturn(List.of(sf1, sf2));

      SongDTO result = repertoireService.createSong(orgId, request);

      assertEquals(5L, result.getId());
      assertEquals(orgId, result.getOrganizationId());
      assertEquals(List.of("tagA", "tagB"), result.getTags());
      assertEquals(List.of(100L), result.getSheetFileIds());
      assertEquals(List.of(101L), result.getAudioFileIds());
      assertEquals(1, result.getInstrumentation().size());

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
   void createSong_tooManyFiles_throwsIllegalArgument() {
      Long orgId = 1L;

      Instrument instrument = Instrument.builder().id(10L).name("Violin").build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      List<MultipartFile> manyFiles = new ArrayList<>();
      for (int i = 0; i < 51; i++) {
         manyFiles.add(new MockMultipartFile(
               "files", "sheet-" + i + ".jpg", "image/jpeg", "x".getBytes()));
      }

      var instr = List.of(new SongInstrumentDTO(10L, 1));
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null, instr, null, manyFiles);

      assertThrows(IllegalArgumentException.class, () -> repertoireService.createSong(orgId, request));
      verify(songRepository, never()).save(any(Song.class));
   }

   @Test
   void createSong_nullInstrumentation_throwsIllegalArgument() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null,
            null, null, null);

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void createSong_emptyInstrumentation_throwsIllegalArgument() {
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null,
            List.<SongInstrumentDTO>of(), null, null);

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void createSong_instrumentNotFound_throwsEntityNotFound() {
      var instr2 = List.of(new SongInstrumentDTO(10L, 1));
      SongCreateRequestDTO request = new SongCreateRequestDTO(
            "Title", null, null, null, null, instr2, null, null);

      when(instrumentRepository.findAllById(any())).thenReturn(List.of());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.createSong(1L, request));
   }

   @Test
   void updateSong_notFound_throwsEntityNotFound() {
      when(songRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.updateSong(1L, 1L, new SongUpdateRequestDTO()));
   }

   @Test
   void updateSong_updatesBasicFields_only() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Old").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenAnswer(invocation -> invocation.getArgument(0));

      when(songMapper.toDto(any(Song.class))).thenReturn(new SongDTO());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setTitle("New");
      request.setDescription("Desc");

      repertoireService.updateSong(1L, 1L, request);

      verify(songInstrumentRepository, never()).deleteBySongId(1L);
      verify(songFileRepository, never()).deleteBySongId(1L);
      assertEquals("New", existing.getTitle());
      assertEquals("Desc", existing.getDescription());
   }

   @Test
   void updateSong_withTooManyFiles_throwsIllegalArgument() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      List<Long> manyFileIds = new ArrayList<>();
      List<StoredFile> manyStoredFiles = new ArrayList<>();
      for (long i = 1; i <= 51; i++) {
         manyFileIds.add(i);
         manyStoredFiles.add(StoredFile.builder().id(i).fileType(FileType.PHOTO).build());
      }
      when(storedFileRepository.findAllById(any())).thenReturn(manyStoredFiles);

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(manyFileIds);

      assertThrows(IllegalArgumentException.class, () -> repertoireService.updateSong(1L, 1L, request));
      verify(songFileRepository, never()).deleteBySongId(1L);
   }

   @Test
   void updateSong_withInstrumentation_replacesInstrumentation() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      Instrument instrument = Instrument.builder().id(10L).name("Violin").build();
      when(instrumentRepository.findAllById(any())).thenReturn(List.of(instrument));

      when(songMapper.toDto(any(Song.class))).thenReturn(new SongDTO());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setInstrumentation(List.of(new SongInstrumentDTO(10L, 2)));

      repertoireService.updateSong(1L, 1L, request);

      verify(songInstrumentRepository).deleteBySongId(1L);
      verify(songInstrumentRepository).saveAll(any());
   }

   @Test
   void updateSong_withEmptyInstrumentation_throwsIllegalArgument() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setInstrumentation(List.of());

      assertThrows(
            IllegalArgumentException.class,
            () -> repertoireService.updateSong(1L, 1L, request));
   }

   @Test
   void updateSong_withFileIds_clearsAndRecreatesFiles() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      Map<Long, StoredFile> fileMap = Map.of(
            101L, StoredFile.builder().id(101L).fileType(FileType.AUDIO).build(),
            102L, StoredFile.builder().id(102L).fileType(FileType.PHOTO).build());
      when(storedFileRepository.findAllById(any())).thenAnswer(invocation -> {
         Iterable<Long> ids = invocation.getArgument(0);
         List<StoredFile> out = new ArrayList<>();
         for (Long id : ids) {
            StoredFile f = fileMap.get(id);
            if (f != null) out.add(f);
         }
         return out;
      });

      SongFile existingAudio = new SongFile();
      existingAudio.setSongId(1L);
      existingAudio.setFileId(101L);
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of(existingAudio));

      when(songMapper.toDto(any(Song.class))).thenReturn(new SongDTO());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of(102L));

      repertoireService.updateSong(1L, 1L, request);

      verify(songFileRepository).deleteBySongId(1L);
      verify(songFileRepository).saveAll(any());
   }

   @Test
   void updateSong_withEmptyFileIds_clearsAllFiles() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));
      when(songRepository.save(any(Song.class))).thenReturn(existing);

      SongFile sfSheet = new SongFile();
      sfSheet.setSongId(1L);
      sfSheet.setFileId(100L);
      SongFile sfAudio = new SongFile();
      sfAudio.setSongId(1L);
      sfAudio.setFileId(101L);
      lenient().when(songFileRepository.findBySongId(1L)).thenReturn(List.of(sfSheet, sfAudio));

      when(songMapper.toDto(any(Song.class))).thenReturn(new SongDTO());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of());

      repertoireService.updateSong(1L, 1L, request);

      verify(songFileRepository).deleteBySongId(1L);
      verify(songFileRepository, never()).saveAll(any());
   }

   @Test
   void updateSong_filesNotFound_throwsEntityNotFound() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      when(storedFileRepository.findAllById(any())).thenReturn(List.of());

      SongUpdateRequestDTO request = new SongUpdateRequestDTO();
      request.setFileIds(List.of(100L));

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.updateSong(1L, 1L, request));
   }

   @Test
   void deleteSong_notFound_throwsEntityNotFound() {
      when(songRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.deleteSong(1L, 1L));
   }

   @Test
   void deleteSong_existing_deletesAndBroadcastsWs() {
      Song existing = Song.builder().id(1L).organizationId(1L).title("Song").build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(existing));

      repertoireService.deleteSong(1L, 1L);

      verify(songRepository).deleteById(1L);
      verify(messagingTemplate).convertAndSend(
            "/topic/organizations/1/repertoire",
            (Object) Map.of("type", "SONG_DELETED", "songId", 1L));
   }

   @Test
   void getSong_notFound_throwsEntityNotFound() {
      when(songRepository.findById(1L)).thenReturn(Optional.empty());

      assertThrows(
            EntityNotFoundException.class,
            () -> repertoireService.getSong(1L, 1L));
   }

   @Test
   void getSong_returnsDtoWithInstrumentationAndFiles() {
      Song song = Song.builder()
            .id(1L).organizationId(1L).title("Song")
            .tags(java.util.Set.of("tag1")).build();
      when(songRepository.findById(1L)).thenReturn(Optional.of(song));

      SongDTO baseDto = new SongDTO();
      baseDto.setId(1L);
      baseDto.setOrganizationId(1L);
      when(songMapper.toDto(any(Song.class))).thenReturn(baseDto);

      SongInstrument si = new SongInstrument();
      si.setSongId(1L);
      si.setInstrumentId(10L);
      si.setCount(2);
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of(si));

      SongFile sf = new SongFile();
      sf.setSongId(1L);
      sf.setFileId(100L);
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of(sf));

      when(storedFileRepository.findAllById(any())).thenReturn(
            List.of(StoredFile.builder().id(100L).fileType(FileType.PHOTO).build()));

      SongDTO dto = repertoireService.getSong(1L, 1L);

      assertEquals(1, dto.getInstrumentation().size());
      assertEquals(10L, dto.getInstrumentation().getFirst().getInstrumentId());
      assertEquals(2, dto.getInstrumentation().getFirst().getCount());

      assertEquals(List.of("tag1"), dto.getTags());
      assertEquals(List.of(100L), dto.getSheetFileIds());
      assertTrue(dto.getAudioFileIds().isEmpty());
   }

   @Test
   void getSongsByOrganization_returnsPagedDtosWithInstrumentationAndFiles() {
      Song song1 = Song.builder().id(1L).organizationId(1L).title("S1").build();
      Song song2 = Song.builder().id(2L).organizationId(1L).title("S2").build();

      Page<Song> page = new PageImpl<>(List.of(song1, song2), PageRequest.of(0, 10), 2);
      when(songRepository.findByOrganizationId(1L,
            PageRequest.of(0, 10,
                  org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
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

   @Test
   void searchSongs_emptyQuery_returnsMatchingSongs() {
      Long orgId = 1L;

      Song song = Song.builder().id(1L).organizationId(orgId).title("Symphony").build();

      Page<Song> page = new PageImpl<>(List.of(song), PageRequest.of(0, 10), 1);
      when(songRepository.findAll(
            org.mockito.Mockito.<org.springframework.data.jpa.domain.Specification<Song>>any(),
            any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(page);

      SongDTO dto = new SongDTO();
      dto.setId(1L);
      when(songMapper.toDto(song)).thenReturn(dto);
      when(songInstrumentRepository.findBySongId(1L)).thenReturn(List.of());
      when(songFileRepository.findBySongId(1L)).thenReturn(List.of());

      Page<SongDTO> result = repertoireService.searchSongs(orgId, null, PageRequest.of(0, 10));

      assertEquals(1, result.getTotalElements());
      assertEquals(1L, result.getContent().getFirst().getId());
   }

   @Test
   void getOrganizationSongTags_returnsUniqueSortedTags() {
      when(songRepository.findDistinctTagsByOrganizationId(1L))
            .thenReturn(List.of("classic", "warmup"));

      List<String> tags = repertoireService.getOrganizationSongTags(1L);

      assertEquals(List.of("classic", "warmup"), tags);
   }
}
