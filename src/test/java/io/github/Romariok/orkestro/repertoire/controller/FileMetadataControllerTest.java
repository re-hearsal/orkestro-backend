package io.github.Romariok.orkestro.repertoire.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileType;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class FileMetadataControllerTest {

    @Mock
    private StoredFileRepository storedFileRepository;

    @InjectMocks
    private FileMetadataController fileMetadataController;

    @Test
    void getFileInfo_pdfFile_returnsOk() {
        StoredFile file = StoredFile.builder()
                .id(1L).name("score.pdf").fileType(FileType.PDF).size(2048L).build();
        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(file));

        var result = fileMetadataController.getFileInfo(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1L, result.getBody().getId());
        assertEquals("score.pdf", result.getBody().getName());
        assertEquals(FileType.PDF, result.getBody().getFileType());
        assertEquals(2048L, result.getBody().getSize());
    }

    @Test
    void getFileInfo_audioFile_returnsOk() {
        StoredFile file = StoredFile.builder()
                .id(2L).name("track.mp3").fileType(FileType.AUDIO).size(5120L).build();
        when(storedFileRepository.findById(2L)).thenReturn(Optional.of(file));

        var result = fileMetadataController.getFileInfo(2L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(FileType.AUDIO, result.getBody().getFileType());
        assertEquals(5120L, result.getBody().getSize());
    }

    @Test
    void getFileInfo_notFound_throwsEntityNotFoundException() {
        when(storedFileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> fileMetadataController.getFileInfo(99L));
    }
}
