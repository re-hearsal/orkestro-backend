package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
}
