package io.github.Romariok.orkestro.repertoire.controller;

import io.github.Romariok.orkestro.repertoire.dto.FileMetadataDTO;
import io.github.Romariok.orkestro.utils.exception.ApiErrorResponse;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "API для управления файлами. Gonna be deprecated eventually")
public class FileMetadataController {

    private final StoredFileRepository storedFileRepository;

    @Operation(summary = "Получить метаданные файла", description = "Возвращает метаданные файла по его ID.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Метаданные файла",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileMetadataDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Не аутентифицирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Файл не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{fileId}/info")
    public ResponseEntity<FileMetadataDTO> getFileInfo(@PathVariable @Positive Long fileId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));
        FileMetadataDTO dto = FileMetadataDTO.builder()
                .id(file.getId())
                .name(file.getName())
                .fileType(file.getFileType())
                .size(file.getSize())
                .build();
        return ResponseEntity.ok(dto);
    }
}
