package io.github.Romariok.orkestro.utils.file.controller;

import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.dto.FileUploadRequestDTO;
import io.github.Romariok.orkestro.utils.file.dto.FileUploadResponseDTO;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class FileController {

      private final FileStorageService fileStorageService;
      private final FileReferenceService fileReferenceService;

      @PostMapping("/upload")
      public ResponseEntity<FileUploadResponseDTO> upload(
                  @Valid @ModelAttribute FileUploadRequestDTO request) {
            if (request.getUploadedByUserId() == null && !isAuthenticated()) {
                  throw new BusinessException("Authentication required when uploadedByUserId is not provided");
            }
            StoredFile storedFile = request.getUploadedByUserId() != null
                        ? fileStorageService.upload(
                              request.getFile(),
                              request.getFileType(),
                              request.getUploadedByUserId())
                        : fileStorageService.uploadForCurrentUser(
                              request.getFile(),
                              request.getFileType());
            FileUploadResponseDTO response = new FileUploadResponseDTO(
                        storedFile.getId(),
                        storedFile.getName(),
                        storedFile.getFileType(),
                        storedFile.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }

   @DeleteMapping("/{fileId}")
   public ResponseEntity<Void> delete(@PathVariable @Positive Long fileId) {
      if (fileReferenceService.isFileReferenced(fileId)) {
         throw new BusinessException("Cannot delete file that is still attached to entities");
      }
      fileStorageService.delete(fileId);
      return ResponseEntity.noContent().build();
   }

   private boolean isAuthenticated() {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return authentication != null
            && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal());
   }
}
