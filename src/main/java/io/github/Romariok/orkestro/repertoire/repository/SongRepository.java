package io.github.Romariok.orkestro.repertoire.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.repertoire.models.Song;

@Repository
public interface SongRepository extends JpaRepository<Song, Long>, JpaSpecificationExecutor<Song> {

   Page<Song> findByOrganizationId(Long organizationId, Pageable pageable);

   void deleteByOrganizationId(Long organizationId);
}
