package io.github.Romariok.orkestro.repertoire.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import io.github.Romariok.orkestro.repertoire.models.Song;

@Repository
public interface SongRepository extends JpaRepository<Song, Long>, JpaSpecificationExecutor<Song> {

   Page<Song> findByOrganizationId(Long organizationId, Pageable pageable);

   void deleteByOrganizationId(Long organizationId);

   @Query("""
         select distinct tag
         from Song s
         join s.tags tag
         where s.organizationId = :organizationId
         order by tag
         """)
   List<String> findDistinctTagsByOrganizationId(@Param("organizationId") Long organizationId);
}
