package io.github.Romariok.orkestro.event.repository;

import io.github.Romariok.orkestro.event.models.EventSong;
import io.github.Romariok.orkestro.event.models.EventSongId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSongRepository extends JpaRepository<EventSong, EventSongId> {

    List<EventSong> findByEventId(Long eventId);

    List<EventSong> findByEventIdIn(List<Long> eventIds);

    void deleteByEventId(Long eventId);
}
