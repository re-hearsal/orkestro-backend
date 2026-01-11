package io.github.Romariok.orkestro.repertoire.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.repertoire.models.SongFile;
import io.github.Romariok.orkestro.repertoire.models.SongFileId;

@Repository
public interface SongFileRepository extends JpaRepository<SongFile, SongFileId> {

   List<SongFile> findBySongId(Long songId);

   void deleteBySongId(Long songId);
}
