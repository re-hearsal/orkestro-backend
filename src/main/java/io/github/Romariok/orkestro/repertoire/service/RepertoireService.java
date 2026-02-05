package io.github.Romariok.orkestro.repertoire.service;

import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongFileUploadRequestDTO;
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
import io.github.Romariok.orkestro.repertoire.specification.SongSpecifications;
import io.github.Romariok.orkestro.user.models.Instrument;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileTypeDetector;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RepertoireService {

   private static final int MAX_SONG_FILES = 50;

   private final OrganizationRepository organizationRepository;
   private final SongRepository songRepository;
   private final SongInstrumentRepository songInstrumentRepository;
   private final SongFileRepository songFileRepository;
   private final InstrumentRepository instrumentRepository;
   private final FileStorageService fileStorageService;
   private final StoredFileRepository storedFileRepository;
   private final SongMapper songMapper;

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'REPERTOIRE_CREATE_SONG')")
   public SongDTO createSong(Long organizationId, SongCreateRequestDTO request) {
      requireOrganizationExists(organizationId);
      validateInstruments(request.getInstrumentation());

      List<String> normalizedTags = normalizeTags(request.getTags());
      List<Long> uploadedFileIds = uploadSongFilesForCreate(
            request.getSheetFiles(),
            request.getAudioFiles());

      Song song = Song.builder()
            .organizationId(organizationId)
            .title(normalizeRequiredString(request.getTitle(), "title"))
            .composer(normalizeNullableString(request.getComposer()))
            .durationSeconds(request.getDurationSeconds())
            .description(normalizeNullableString(request.getDescription()))
            .videoUrl(normalizeNullableString(request.getVideoUrl()))
            .createdAt(Instant.now())
            .build();

      if (!normalizedTags.isEmpty()) {
         song.getTags().addAll(normalizedTags);
      }

      Song saved = songRepository.save(song);

      saveInstrumentation(saved.getId(), request.getInstrumentation());
      validateSongTotalFilesCount(uploadedFileIds);
      saveSongFiles(saved.getId(), uploadedFileIds);

      return buildSongDto(saved);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'REPERTOIRE_EDIT_SONG')")
   public SongDTO updateSong(Long organizationId, Long songId, SongUpdateRequestDTO request) {
      requireOrganizationExists(organizationId);
      Song song = getSongInOrganizationOrThrow(organizationId, songId);

      if (request.getTitle() != null) {
         song.setTitle(normalizeRequiredString(request.getTitle(), "title"));
      }
      if (request.getComposer() != null) {
         song.setComposer(normalizeNullableString(request.getComposer()));
      }
      if (request.getDurationSeconds() != null) {
         song.setDurationSeconds(request.getDurationSeconds());
      }
      if (request.getDescription() != null) {
         song.setDescription(normalizeNullableString(request.getDescription()));
      }
      if (request.getVideoUrl() != null) {
         song.setVideoUrl(normalizeNullableString(request.getVideoUrl()));
      }

      if (request.getInstrumentation() != null) {
         validateInstruments(request.getInstrumentation());
         songInstrumentRepository.deleteBySongId(songId);
         saveInstrumentation(songId, request.getInstrumentation());
      }

      if (request.getTags() != null) {
         List<String> normalizedTags = normalizeTags(request.getTags());
         song.getTags().clear();
         song.getTags().addAll(normalizedTags);
      }

      if (request.getSheetFileIds() != null || request.getAudioFileIds() != null) {
         FileBuckets current = bucketizeSongFiles(songId);

         List<Long> targetSheetFileIds = request.getSheetFileIds() != null
               ? normalizeFileIds(request.getSheetFileIds())
               : current.sheetFileIds();
         List<Long> targetAudioFileIds = request.getAudioFileIds() != null
               ? normalizeFileIds(request.getAudioFileIds())
               : current.audioFileIds();

         validateSongFiles(targetSheetFileIds, targetAudioFileIds);

         List<Long> merged = new ArrayList<>();
         merged.addAll(targetSheetFileIds);
         merged.addAll(targetAudioFileIds);
         merged.addAll(current.otherFileIds());

         validateSongTotalFilesCount(merged);
         songFileRepository.deleteBySongId(songId);
         saveSongFiles(songId, merged);
      }

      Song saved = songRepository.save(song);
      return buildSongDto(saved);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'REPERTOIRE_EDIT_SONG')")
   public SongDTO uploadAndAttachSongFile(
         Long organizationId,
         Long songId,
         SongFileUploadRequestDTO request) {
      requireOrganizationExists(organizationId);
      Song song = getSongInOrganizationOrThrow(organizationId, songId);

      if (request == null || request.getFile() == null) {
         throw new IllegalArgumentException("file is required");
      }
      if (request.getFile().isEmpty() || request.getFile().getSize() <= 0) {
         throw new IllegalArgumentException("file is required");
      }
      String originalName = request.getFile().getOriginalFilename();
      if (originalName == null || originalName.isBlank()) {
         throw new IllegalArgumentException("file name is required");
      }

      FileType detectedType = FileTypeDetector.detect(request.getFile());
      if (detectedType != FileType.PDF
            && detectedType != FileType.PHOTO
            && detectedType != FileType.AUDIO) {
         throw new IllegalArgumentException("Unsupported fileType: " + detectedType);
      }

      int existingCount = songFileRepository.findBySongId(songId).size();
      if (existingCount >= MAX_SONG_FILES) {
         throw new IllegalArgumentException(
               "Song cannot have more than " + MAX_SONG_FILES + " files");
      }

      StoredFile stored = fileStorageService.uploadForCurrentUser(
            request.getFile(),
            detectedType);

      SongFile link = new SongFile();
      link.setSongId(songId);
      link.setFileId(stored.getId());
      songFileRepository.save(link);

      return buildSongDto(song);
   }

   @Transactional
   @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'REPERTOIRE_DELETE_SONG')")
   public void deleteSong(Long organizationId, Long songId) {
      requireOrganizationExists(organizationId);
      Song song = getSongInOrganizationOrThrow(organizationId, songId);
      songRepository.deleteById(song.getId());
   }

   @Transactional(readOnly = true)
   @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
   public SongDTO getSong(Long organizationId, Long songId) {
      requireOrganizationExists(organizationId);
      Song song = getSongInOrganizationOrThrow(organizationId, songId);
      return buildSongDto(song);
   }

   /**
    * Получить песни организации с пагинацией.
    */
   @Transactional(readOnly = true)
   @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
   public Page<SongDTO> getSongsByOrganization(Long organizationId, int page, int size) {
      requireOrganizationExists(organizationId);
      Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
      Page<Song> songsPage = songRepository.findByOrganizationId(organizationId, pageable);

      List<SongDTO> dtos = songsPage.getContent().stream()
            .map(this::buildSongDto)
            .toList();

      return new PageImpl<>(dtos, pageable, songsPage.getTotalElements());
   }

   @Transactional(readOnly = true)
   @PreAuthorize("@organizationPermissionChecker.isAcceptedOrganizationMember(#organizationId)")
   public Page<SongDTO> searchSongs(Long organizationId, String query, Pageable pageable) {
      requireOrganizationExists(organizationId);

      Pageable mappedPageable = mapSongSort(pageable);

      Specification<Song> spec = Specification.where(SongSpecifications.isInOrganization(organizationId))
            .and(SongSpecifications.titleOrComposerOrTagContainsIgnoreCase(query));

      Page<Song> songsPage = songRepository.findAll(spec, mappedPageable);
      return songsPage.map(this::buildSongDto);
   }

   private Pageable mapSongSort(Pageable pageable) {
      if (pageable == null) {
         return PageRequest.of(0, 20);
      }
      Sort sort = pageable.getSort();
      if (sort == null || sort.isUnsorted()) {
         return pageable;
      }

      List<Sort.Order> mapped = new ArrayList<>();
      for (Sort.Order order : sort) {
         String prop = order.getProperty();
         // We don't remap anything here, only validate allowed properties.
         switch (prop) {
            case "id", "title", "composer", "durationSeconds", "createdAt" -> {
            }
            default -> throw new IllegalArgumentException("Unsupported sort property: " + prop);
         }

         Sort.Order mappedOrder = new Sort.Order(order.getDirection(), prop)
               .with(order.getNullHandling());
         if (order.isIgnoreCase()) {
            mappedOrder = mappedOrder.ignoreCase();
         }
         mapped.add(mappedOrder);
      }

      return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mapped));
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

      FileBuckets buckets = bucketizeFileIds(fileIds);

      dto.setInstrumentation(instrumentation);
      dto.setTags(song.getTags().stream()
            .sorted(Comparator.naturalOrder())
            .toList());
      dto.setSheetFileIds(buckets.sheetFileIds());
      dto.setAudioFileIds(buckets.audioFileIds());
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

      List<Long> normalized = normalizeFileIds(fileIds);
      if (normalized.isEmpty()) {
         return;
      }

      validateSongTotalFilesCount(normalized);

      List<SongFile> entities = normalized.stream()
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

   private void requireOrganizationExists(Long organizationId) {
      if (!organizationRepository.existsById(organizationId)) {
         throw new EntityNotFoundException("Organization not found: " + organizationId);
      }
   }

   private Song getSongInOrganizationOrThrow(Long organizationId, Long songId) {
      Song song = songRepository
            .findById(songId)
            .orElseThrow(() -> new EntityNotFoundException("Song not found: " + songId));
      if (!organizationId.equals(song.getOrganizationId())) {
         throw new EntityNotFoundException("Song not found: " + songId);
      }
      return song;
   }

   private String normalizeRequiredString(String value, String field) {
      if (value == null) {
         throw new IllegalArgumentException(field + " is required");
      }
      String normalized = value.trim();
      if (normalized.isBlank()) {
         throw new IllegalArgumentException(field + " cannot be blank");
      }
      return normalized;
   }

   private String normalizeNullableString(String value) {
      if (value == null) {
         return null;
      }
      String normalized = value.trim();
      if (normalized.isBlank()) {
         return null;
      }
      return normalized;
   }

   private List<String> normalizeTags(List<String> tags) {
      if (tags == null || tags.isEmpty()) {
         return List.of();
      }
      List<String> normalized = tags.stream()
            .filter(t -> t != null)
            .map(String::trim)
            .filter(t -> !t.isBlank())
            .toList();

      Set<String> uniq = new HashSet<>();
      for (String t : normalized) {
         String key = t.toLowerCase(Locale.ROOT);
         if (!uniq.add(key)) {
            throw new IllegalArgumentException("Tags must not contain duplicates");
         }
      }
      return normalized.stream().sorted().toList();
   }

   private List<Long> normalizeFileIds(Collection<Long> fileIds) {
      if (fileIds == null || fileIds.isEmpty()) {
         return List.of();
      }
      return fileIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
   }

   private void validateSongFiles(List<Long> sheetFileIds, List<Long> audioFileIds) {
      Set<Long> sheetSet = new HashSet<>(sheetFileIds == null ? List.of() : sheetFileIds);
      Set<Long> audioSet = new HashSet<>(audioFileIds == null ? List.of() : audioFileIds);

      Set<Long> overlap = new HashSet<>(sheetSet);
      overlap.retainAll(audioSet);
      if (!overlap.isEmpty()) {
         throw new IllegalArgumentException("sheetFileIds and audioFileIds must not overlap");
      }

      int totalUnique = sheetSet.size() + audioSet.size();
      if (totalUnique > MAX_SONG_FILES) {
         throw new IllegalArgumentException(
               "Song cannot have more than " + MAX_SONG_FILES + " files");
      }

      validateFilesByTypes(sheetFileIds, Set.of(FileType.PDF, FileType.PHOTO), "sheetFileIds");
      validateFilesByTypes(audioFileIds, Set.of(FileType.AUDIO), "audioFileIds");
   }

   private List<Long> uploadSongFilesForCreate(
         List<MultipartFile> sheetFiles,
         List<MultipartFile> audioFiles) {
      int sheetCount = sheetFiles == null ? 0 : sheetFiles.size();
      int audioCount = audioFiles == null ? 0 : audioFiles.size();
      int totalCount = sheetCount + audioCount;
      if (totalCount > MAX_SONG_FILES) {
         throw new IllegalArgumentException(
               "Song cannot have more than " + MAX_SONG_FILES + " files");
      }

      validateMultipartFiles(sheetFiles, "sheetFiles");
      validateMultipartFiles(audioFiles, "audioFiles");

      List<Long> uploadedIds = new ArrayList<>();

      uploadMultipartFiles(uploadedIds, sheetFiles, Set.of(FileType.PDF, FileType.PHOTO), "sheetFiles");
      uploadMultipartFiles(uploadedIds, audioFiles, Set.of(FileType.AUDIO), "audioFiles");

      return uploadedIds;
   }

   private void validateMultipartFiles(
         List<MultipartFile> files,
         String filesFieldName) {
      if (files == null || files.isEmpty()) {
         return;
      }

      for (int i = 0; i < files.size(); i++) {
         MultipartFile f = files.get(i);
         if (f == null || f.isEmpty() || f.getSize() <= 0) {
            throw new IllegalArgumentException(filesFieldName + "[" + i + "] file is required");
         }
         String originalName = f.getOriginalFilename();
         if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException(filesFieldName + "[" + i + "] file name is required");
         }
      }
   }

   private void uploadMultipartFiles(
         List<Long> uploadedIds,
         List<MultipartFile> files,
         Set<FileType> allowedTypes,
         String filesFieldName) {
      if (files == null || files.isEmpty()) {
         return;
      }

      for (int i = 0; i < files.size(); i++) {
         MultipartFile file = files.get(i);
         FileType detected = FileTypeDetector.detect(file);
         if (!allowedTypes.contains(detected)) {
            throw new IllegalArgumentException(filesFieldName + "[" + i + "] has unsupported fileType: " + detected);
         }
         StoredFile stored = fileStorageService.uploadForCurrentUser(file, detected);
         uploadedIds.add(stored.getId());
      }
   }

   private void validateSongTotalFilesCount(Collection<Long> fileIds) {
      if (fileIds == null || fileIds.isEmpty()) {
         return;
      }
      int uniqueCount = normalizeFileIds(fileIds).size();
      if (uniqueCount > MAX_SONG_FILES) {
         throw new IllegalArgumentException(
               "Song cannot have more than " + MAX_SONG_FILES + " files");
      }
   }

   private void validateFilesByTypes(List<Long> fileIds, Set<FileType> allowed, String fieldName) {
      if (fileIds == null || fileIds.isEmpty()) {
         return;
      }

      Set<Long> uniqueIds = new HashSet<>(fileIds);
      List<StoredFile> files = storedFileRepository.findAllById(uniqueIds);
      if (files.size() != uniqueIds.size()) {
         throw new EntityNotFoundException("One or more files not found for ids: " + uniqueIds);
      }

      List<Long> invalidTypeIds = files.stream()
            .filter(f -> f.getFileType() == null || !allowed.contains(f.getFileType()))
            .map(StoredFile::getId)
            .sorted()
            .toList();
      if (!invalidTypeIds.isEmpty()) {
         throw new IllegalArgumentException(
               fieldName + " contains unsupported file types for ids: " + invalidTypeIds);
      }
   }

   private FileBuckets bucketizeSongFiles(Long songId) {
      List<Long> fileIds = songFileRepository.findBySongId(songId).stream()
            .map(SongFile::getFileId)
            .toList();
      return bucketizeFileIds(fileIds);
   }

   private FileBuckets bucketizeFileIds(List<Long> fileIds) {
      if (fileIds == null || fileIds.isEmpty()) {
         return new FileBuckets(List.of(), List.of(), List.of());
      }
      Set<Long> unique = new HashSet<>(fileIds);
      List<StoredFile> files = storedFileRepository.findAllById(unique);

      List<Long> sheet = new ArrayList<>();
      List<Long> audio = new ArrayList<>();
      List<Long> other = new ArrayList<>();

      for (StoredFile f : files) {
         if (f.getFileType() == FileType.PDF || f.getFileType() == FileType.PHOTO) {
            sheet.add(f.getId());
         } else if (f.getFileType() == FileType.AUDIO) {
            audio.add(f.getId());
         } else {
            other.add(f.getId());
         }
      }

      sheet.sort(Comparator.naturalOrder());
      audio.sort(Comparator.naturalOrder());
      other.sort(Comparator.naturalOrder());
      return new FileBuckets(sheet, audio, other);
   }

   private record FileBuckets(List<Long> sheetFileIds, List<Long> audioFileIds, List<Long> otherFileIds) {
   }
}
