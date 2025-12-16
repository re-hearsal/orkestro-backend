package io.github.Romariok.orkestro.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "song_files")
@IdClass(SongFileId.class)
public class SongFile {

    @Id
    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Id
    @Column(name = "file_id", nullable = false)
    private Long fileId;
}


