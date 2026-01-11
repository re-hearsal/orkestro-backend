package io.github.Romariok.orkestro.event.models;

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
@Table(name = "event_songs")
@IdClass(EventSongId.class)
public class EventSong {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Id
    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Column(name = "position")
    private Integer position;
}


