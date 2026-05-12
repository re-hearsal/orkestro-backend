package io.github.Romariok.orkestro.repertoire.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.repertoire.models.SongInstrument;
import io.github.Romariok.orkestro.repertoire.models.SongInstrumentId;

@Repository
public interface SongInstrumentRepository extends JpaRepository<SongInstrument, SongInstrumentId> {

   List<SongInstrument> findBySongId(Long songId);

   void deleteBySongId(Long songId);
}
