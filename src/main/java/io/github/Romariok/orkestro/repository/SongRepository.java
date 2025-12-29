package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

   Page<Song> findByOrganizationId(Long organizationId, Pageable pageable);
}
