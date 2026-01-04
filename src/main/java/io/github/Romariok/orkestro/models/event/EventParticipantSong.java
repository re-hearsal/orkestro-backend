package io.github.Romariok.orkestro.models.event;

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
@Table(name = "event_participant_songs")
@IdClass(EventParticipantSongId.class)
public class EventParticipantSong {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Id
    @Column(name = "song_id", nullable = false)
    private Long songId;

    @Id
    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Id
    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}


