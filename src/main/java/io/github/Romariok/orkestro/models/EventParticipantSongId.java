package io.github.Romariok.orkestro.models;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class EventParticipantSongId implements Serializable {

    private Long eventId;
    private Long songId;
    private Long instrumentId;
    private Integer position;
}


