package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.SongFile;
import io.github.Romariok.orkestro.models.SongFileId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongFileRepository extends JpaRepository<SongFile, SongFileId> {

   List<SongFile> findBySongId(Long songId);

   void deleteBySongId(Long songId);
}
