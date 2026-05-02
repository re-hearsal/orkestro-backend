package io.github.Romariok.orkestro.repertoire.dto;

import io.github.Romariok.orkestro.utils.file.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadataDTO {
    private Long id;
    private String name;
    private FileType fileType;
    private Long size;
}
