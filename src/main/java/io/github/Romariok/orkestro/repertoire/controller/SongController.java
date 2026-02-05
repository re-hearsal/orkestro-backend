package io.github.Romariok.orkestro.repertoire.controller;

import io.github.Romariok.orkestro.repertoire.dto.SongCreateRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongFileUploadRequestDTO;
import io.github.Romariok.orkestro.repertoire.dto.SongUpdateRequestDTO;
import io.github.Romariok.orkestro.repertoire.service.RepertoireService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/repertoire/songs")
public class SongController {

   private final RepertoireService repertoireService;

   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<SongDTO> createSong(
         @PathVariable @Positive Long organizationId,
         @Valid @ModelAttribute SongCreateRequestDTO request) {
      SongDTO created = repertoireService.createSong(organizationId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
   }

   @PostMapping(value = "/{songId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<SongDTO> uploadAndAttachFile(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long songId,
         @Valid @ModelAttribute SongFileUploadRequestDTO request) {
      SongDTO updated = repertoireService.uploadAndAttachSongFile(organizationId, songId, request);
      return ResponseEntity.status(HttpStatus.CREATED).body(updated);
   }

   @PutMapping("/{songId}")
   public ResponseEntity<SongDTO> updateSong(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long songId,
         @Valid @RequestBody SongUpdateRequestDTO request) {
      return ResponseEntity.ok(repertoireService.updateSong(organizationId, songId, request));
   }

   @DeleteMapping("/{songId}")
   public ResponseEntity<Void> deleteSong(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long songId) {
      repertoireService.deleteSong(organizationId, songId);
      return ResponseEntity.noContent().build();
   }

   @GetMapping("/{songId}")
   public ResponseEntity<SongDTO> getSong(
         @PathVariable @Positive Long organizationId,
         @PathVariable @Positive Long songId) {
      return ResponseEntity.ok(repertoireService.getSong(organizationId, songId));
   }

   @GetMapping("/page")
   public ResponseEntity<Page<SongDTO>> searchSongsPage(
         @PathVariable @Positive Long organizationId,
         @RequestParam(required = false) String query,
         @PageableDefault(size = 20) Pageable pageable) {
      return ResponseEntity.ok(repertoireService.searchSongs(organizationId, query, pageable));
   }
}

