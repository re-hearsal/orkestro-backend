package io.github.Romariok.orkestro.service;

import io.github.Romariok.orkestro.config.MinioProperties;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.enums.FileType;
import io.github.Romariok.orkestro.repository.StoredFileRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.exception.InternalServiceException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileStorageService {

   private final MinioClient minioClient;
   private final MinioProperties minioProperties;
   private final StoredFileRepository storedFileRepository;
   private final SecurityUtils securityUtils;

   @Transactional
   public StoredFile uploadForCurrentUser(MultipartFile file, FileType fileType) {
      Long userId = securityUtils.getCurrentUserId();
      return upload(file, fileType, userId);
   }

   @Transactional
   public StoredFile upload(MultipartFile file, FileType fileType, Long uploadedByUserId) {
      String bucket = minioProperties.getBucket();
      String originalName = sanitizeFilename(file.getOriginalFilename());
      String objectName = UUID.randomUUID() + "-" + originalName;

      try (InputStream inputStream = file.getInputStream()) {
         PutObjectArgs putArgs = PutObjectArgs.builder()
               .bucket(bucket)
               .object(objectName)
               .stream(inputStream, file.getSize(), -1)
               .contentType(file.getContentType())
               .build();

         minioClient.putObject(putArgs);

         StoredFile stored = StoredFile.builder()
               .name(originalName)
               .fileType(fileType != null ? fileType : detectFileType(file))
               .bucketName(bucket)
               .objectName(objectName)
               .size(file.getSize())
               .createdAt(Instant.now())
               .uploadedByUserId(uploadedByUserId)
               .build();

         return storedFileRepository.save(stored);
      } catch (Exception e) {
         throw new InternalServiceException(
               "Error uploading file: " + originalName, e);
      }
   }

   @Transactional(readOnly = true)
   public InputStream download(Long fileId) {
      StoredFile storedFile = storedFileRepository
            .findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));

      try {
         GetObjectArgs args = GetObjectArgs.builder()
               .bucket(storedFile.getBucketName())
               .object(storedFile.getObjectName())
               .build();
         return minioClient.getObject(args);
      } catch (Exception e) {
         throw new InternalServiceException("Error downloading file: " + fileId, e);
      }
   }

   @Transactional
   public void delete(Long fileId) {
      StoredFile storedFile = storedFileRepository
            .findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));

      try {
         RemoveObjectArgs removeArgs = RemoveObjectArgs.builder()
               .bucket(storedFile.getBucketName())
               .object(storedFile.getObjectName())
               .build();
         minioClient.removeObject(removeArgs);
      } catch (Exception e) {
         throw new InternalServiceException("Error deleting file from storage: " + fileId, e);
      }

      storedFileRepository.delete(storedFile);
   }

   @Transactional(readOnly = true)
   public String generateDownloadUrl(Long fileId, Duration expiry) {
      StoredFile storedFile = storedFileRepository
            .findById(fileId)
            .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));

      int seconds = (int) expiry.getSeconds();
      if (seconds <= 0) {
         seconds = (int) Duration.ofMinutes(15).getSeconds();
      }

      try {
         GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
               .method(Method.GET)
               .bucket(storedFile.getBucketName())
               .object(storedFile.getObjectName())
               .expiry(seconds)
               .build();

         return minioClient.getPresignedObjectUrl(args);
      } catch (Exception e) {
         throw new InternalServiceException(
               "Error generating download URL for file: " + fileId, e);
      }
   }

   private String sanitizeFilename(String originalFilename) {
      String name = originalFilename != null ? originalFilename : "file";
      name = name.replace("\\", "/");
      int lastSlash = name.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash < name.length() - 1) {
         name = name.substring(lastSlash + 1);
      }
      return name;
   }

   private FileType detectFileType(MultipartFile file) {
      String contentType = file.getContentType();
      String filename = file.getOriginalFilename();
      String lowerName = filename != null ? filename.toLowerCase(Locale.ROOT) : "";

      if (contentType != null) {
         if (contentType.startsWith("image/")) {
            return FileType.PHOTO;
         }
         if (contentType.startsWith("audio/")) {
            return FileType.AUDIO;
         }
         if (contentType.startsWith("video/")) {
            return FileType.VIDEO;
         }
      }

      if (lowerName.endsWith(".pdf")) {
         return FileType.PDF;
      }

      return FileType.OTHER;
   }
}
