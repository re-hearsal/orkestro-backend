package io.github.Romariok.orkestro.utils.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.web.multipart.MultipartFile;

/**
 * Detects {@link FileType} for uploaded files on the backend side.
 *
 * <p>Detection priority:
 * <ol>
 *   <li>Magic bytes (best effort)</li>
 *   <li>MIME type</li>
 *   <li>Filename extension</li>
 * </ol>
 */
public final class FileTypeDetector {

   private FileTypeDetector() {
   }

   public static FileType detect(MultipartFile file) {
      if (file == null) {
         return FileType.OTHER;
      }

      String filename = file.getOriginalFilename();
      String lowerName = filename != null ? filename.toLowerCase(Locale.ROOT) : "";

      FileType byMagic = detectByMagicBytes(file, lowerName);
      if (byMagic != null && byMagic != FileType.OTHER) {
         return byMagic;
      }

      String contentType = file.getContentType();
      if (contentType != null) {
         String ct = contentType.toLowerCase(Locale.ROOT);
         if (ct.equals("application/pdf")) {
            return FileType.PDF;
         }
         if (ct.startsWith("image/")) {
            return FileType.PHOTO;
         }
         if (ct.startsWith("audio/")) {
            return FileType.AUDIO;
         }
         if (ct.startsWith("video/")) {
            return FileType.VIDEO;
         }
      }

      // Extension fallback
      if (lowerName.endsWith(".pdf")) {
         return FileType.PDF;
      }

      if (lowerName.endsWith(".png")
            || lowerName.endsWith(".jpg")
            || lowerName.endsWith(".jpeg")
            || lowerName.endsWith(".gif")
            || lowerName.endsWith(".webp")
            || lowerName.endsWith(".bmp")
            || lowerName.endsWith(".tif")
            || lowerName.endsWith(".tiff")
            || lowerName.endsWith(".svg")) {
         return FileType.PHOTO;
      }

      if (lowerName.endsWith(".mp3")
            || lowerName.endsWith(".wav")
            || lowerName.endsWith(".ogg")
            || lowerName.endsWith(".opus")
            || lowerName.endsWith(".flac")
            || lowerName.endsWith(".m4a")
            || lowerName.endsWith(".aac")) {
         return FileType.AUDIO;
      }

      if (lowerName.endsWith(".mp4")
            || lowerName.endsWith(".mov")
            || lowerName.endsWith(".mkv")
            || lowerName.endsWith(".avi")
            || lowerName.endsWith(".webm")) {
         return FileType.VIDEO;
      }

      return FileType.OTHER;
   }

   private static FileType detectByMagicBytes(MultipartFile file, String lowerName) {
      byte[] header = readHeader(file, 64);
      if (header.length == 0) {
         return FileType.OTHER;
      }

      // PDF: "%PDF-"
      byte[] pdf = "%PDF-".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, pdf)) {
         return FileType.PDF;
      }

      // PNG: 89 50 4E 47 0D 0A 1A 0A
      byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
      if (startsWith(header, png)) {
         return FileType.PHOTO;
      }

      // JPEG: FF D8 FF
      byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
      if (startsWith(header, jpeg)) {
         return FileType.PHOTO;
      }

      // GIF: "GIF87a" / "GIF89a"
      byte[] gif87a = "GIF87a".getBytes(StandardCharsets.US_ASCII);
      byte[] gif89a = "GIF89a".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, gif87a) || startsWith(header, gif89a)) {
         return FileType.PHOTO;
      }

      // RIFF container: WAV / WEBP
      byte[] riff = "RIFF".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, riff) && header.length >= 12) {
         String kind = new String(header, 8, 4, StandardCharsets.US_ASCII);
         if ("WAVE".equals(kind)) {
            return FileType.AUDIO;
         }
         if ("WEBP".equals(kind)) {
            return FileType.PHOTO;
         }
      }

      // OGG: "OggS"
      byte[] oggs = "OggS".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, oggs)) {
         return FileType.AUDIO;
      }

      // FLAC: "fLaC"
      byte[] flac = "fLaC".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, flac)) {
         return FileType.AUDIO;
      }

      // MP3: "ID3" or frame sync 0xFFEx
      byte[] id3 = "ID3".getBytes(StandardCharsets.US_ASCII);
      if (startsWith(header, id3) || (header.length >= 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0)) {
         return FileType.AUDIO;
      }

      // ISO BMFF (mp4/m4a): "....ftyp"
      if (header.length >= 12
            && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') {
         // Prefer extension to decide between AUDIO (m4a) and VIDEO (mp4)
         if (lowerName.endsWith(".m4a")) {
            return FileType.AUDIO;
         }
         return FileType.VIDEO;
      }

      return FileType.OTHER;
   }

   private static byte[] readHeader(MultipartFile file, int maxBytes) {
      try (InputStream in = file.getInputStream()) {
         return in.readNBytes(maxBytes);
      } catch (IOException e) {
         return new byte[0];
      }
   }

   private static boolean startsWith(byte[] data, byte[] prefix) {
      if (data.length < prefix.length) {
         return false;
      }
      for (int i = 0; i < prefix.length; i++) {
         if (data[i] != prefix[i]) {
            return false;
         }
      }
      return true;
   }
}

