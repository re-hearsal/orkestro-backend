package io.github.Romariok.orkestro.utils.file;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByUploadedByUserId(Long uploadedByUserId);

    @Modifying
    @Query("update StoredFile f set f.uploadedByUserId = null where f.uploadedByUserId = :userId")
    int clearUploadedByUserId(@Param("userId") Long userId);
}
