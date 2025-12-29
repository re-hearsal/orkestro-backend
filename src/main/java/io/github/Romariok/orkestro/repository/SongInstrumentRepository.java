package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.SongInstrument;
import io.github.Romariok.orkestro.models.SongInstrumentId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongInstrumentRepository extends JpaRepository<SongInstrument, SongInstrumentId> {

   List<SongInstrument> findBySongId(Long songId);

   void deleteBySongId(Long songId);
}
