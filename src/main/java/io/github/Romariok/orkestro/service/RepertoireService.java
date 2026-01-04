package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.dto.song.SongCreateRequestDTO;
import io.github.Romariok.orkestro.dto.song.SongDTO;
import io.github.Romariok.orkestro.dto.song.SongInstrumentDTO;
import io.github.Romariok.orkestro.dto.song.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.mapper.SongMapper;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.role.Instrument;
import io.github.Romariok.orkestro.models.song.Song;
import io.github.Romariok.orkestro.models.song.SongFile;
import io.github.Romariok.orkestro.models.song.SongInstrument;
import io.github.Romariok.orkestro.repository.InstrumentRepository;
import io.github.Romariok.orkestro.repository.SongFileRepository;
import io.github.Romariok.orkestro.repository.SongInstrumentRepository;
import io.github.Romariok.orkestro.repository.SongRepository;
import io.github.Romariok.orkestro.repository.StoredFileRepository;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepertoireService {

   private final SongRepository songRepository;
   private final SongInstrumentRepository songInstrumentRepository;
   private final SongFileRepository songFileRepository;
   private final InstrumentRepository instrumentRepository;
   private final StoredFileRepository storedFileRepository;
   private final SongMapper songMapper;

   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':REPERTOIRE_CREATE_SONG')")
   public SongDTO createSong(Long organizationId, SongCreateRequestDTO request) {
      validateInstruments(request.getInstrumentation());
      validateFiles(request.getFileIds());

      Song song = Song.builder()
            .organizationId(organizationId)
            .title(request.getTitle())
            .composer(request.getComposer())
            .durationSeconds(request.getDurationSeconds())
            .description(request.getDescription())
            .videoUrl(request.getVideoUrl())
            .createdAt(Instant.now())
            .build();

      Song saved = songRepository.save(song);

      saveInstrumentation(saved.getId(), request.getInstrumentation());
      saveSongFiles(saved.getId(), request.getFileIds());

      return buildSongDto(saved);
   }

   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + "
         + "@songRepository.findById(#songId).orElse(null)?.organizationId + ':REPERTOIRE_EDIT_SONG')")
   public SongDTO updateSong(Long songId, SongUpdateRequestDTO request) {
      Song song = songRepository
            .findById(songId)
            .orElseThrow(() -> new EntityNotFoundException("Song not found: " + songId));

      if (request.getTitle() != null) {
         song.setTitle(request.getTitle());
      }
      if (request.getComposer() != null) {
         song.setComposer(request.getComposer());
      }
      if (request.getDurationSeconds() != null) {
         song.setDurationSeconds(request.getDurationSeconds());
      }
      if (request.getDescription() != null) {
         song.setDescription(request.getDescription());
      }
      if (request.getVideoUrl() != null) {
         song.setVideoUrl(request.getVideoUrl());
      }

      Song saved = songRepository.save(song);

      if (request.getInstrumentation() != null) {
         validateInstruments(request.getInstrumentation());
         songInstrumentRepository.deleteBySongId(songId);
         saveInstrumentation(songId, request.getInstrumentation());
      }

      if (request.getFileIds() != null) {
         validateFiles(request.getFileIds());
         songFileRepository.deleteBySongId(songId);
         saveSongFiles(songId, request.getFileIds());
      }

      return buildSongDto(saved);
   }

   @Transactional
   @PreAuthorize("hasAuthority('CTX_PERM_ORG:' + "
         + "@songRepository.findById(#songId).orElse(null)?.organizationId + ':REPERTOIRE_DELETE_SONG')")
   public void deleteSong(Long songId) {
      if (!songRepository.existsById(songId)) {
         throw new EntityNotFoundException("Song not found: " + songId);
      }
      songRepository.deleteById(songId);
   }

   @Transactional(readOnly = true)
   public SongDTO getSong(Long songId) {
      Song song = songRepository
            .findById(songId)
            .orElseThrow(() -> new EntityNotFoundException("Song not found: " + songId));
      return buildSongDto(song);
   }

   /**
    * Получить песни организации с пагинацией.
    */
   @Transactional(readOnly = true)
   public Page<SongDTO> getSongsByOrganization(Long organizationId, int page, int size) {
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<Song> songsPage = songRepository.findByOrganizationId(organizationId, pageable);

      List<SongDTO> dtos = songsPage.getContent().stream()
            .map(this::buildSongDto)
            .toList();

      return new PageImpl<>(dtos, pageable, songsPage.getTotalElements());
   }

   private SongDTO buildSongDto(Song song) {
      SongDTO dto = songMapper.toDto(song);

      List<SongInstrument> instruments = songInstrumentRepository.findBySongId(song.getId());
      List<SongInstrumentDTO> instrumentation = instruments.stream()
            .map(si -> new SongInstrumentDTO(si.getInstrumentId(), si.getCount()))
            .toList();

      List<Long> fileIds = songFileRepository.findBySongId(song.getId()).stream()
            .map(SongFile::getFileId)
            .toList();

      dto.setInstrumentation(instrumentation);
      dto.setFileIds(fileIds);
      return dto;
   }

   private void saveInstrumentation(Long songId, List<SongInstrumentDTO> instrumentation) {
      if (instrumentation == null || instrumentation.isEmpty()) {
         return;
      }

      List<SongInstrument> entities = instrumentation.stream()
            .map(dto -> {
               SongInstrument si = new SongInstrument();
               si.setSongId(songId);
               si.setInstrumentId(dto.getInstrumentId());
               si.setCount(dto.getCount());
               return si;
            })
            .toList();

      songInstrumentRepository.saveAll(entities);
   }

   private void saveSongFiles(Long songId, List<Long> fileIds) {
      if (fileIds == null || fileIds.isEmpty()) {
         return;
      }

      List<SongFile> entities = fileIds.stream()
            .map(fileId -> {
               SongFile sf = new SongFile();
               sf.setSongId(songId);
               sf.setFileId(fileId);
               return sf;
            })
            .toList();

      songFileRepository.saveAll(entities);
   }

   private void validateInstruments(List<SongInstrumentDTO> instrumentation) {
      if (instrumentation == null || instrumentation.isEmpty()) {
         throw new IllegalArgumentException("Instrumentation cannot be null or empty");
      }
      Set<Long> instrumentIds = instrumentation.stream()
            .map(SongInstrumentDTO::getInstrumentId)
            .collect(Collectors.toCollection(HashSet::new));

      List<Instrument> instruments = instrumentRepository.findAllById(instrumentIds);
      if (instruments.size() != instrumentIds.size()) {
         throw new EntityNotFoundException("One or more instruments not found for ids: " + instrumentIds);
      }
   }

   private void validateFiles(List<Long> fileIds) {
      if (fileIds == null || fileIds.isEmpty()) {
         return;
      }

      Set<Long> uniqueIds = new HashSet<>(fileIds);
      List<StoredFile> files = storedFileRepository.findAllById(uniqueIds);
      if (files.size() != uniqueIds.size()) {
         throw new EntityNotFoundException("One or more files not found for ids: " + uniqueIds);
      }
   }
}
