package io.github.Romariok.orkestro.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.models.song.Song;

@Repository
public interface SongRepository extends JpaRepository<Song, Long> {

   Page<Song> findByOrganizationId(Long organizationId, Pageable pageable);
}
