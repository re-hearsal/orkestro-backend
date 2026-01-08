package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.section.SectionUser;
import io.github.Romariok.orkestro.models.section.SectionUserId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionUserRepository extends JpaRepository<SectionUser, SectionUserId> {

    Optional<SectionUser> findBySectionIdAndUserId(Long sectionId, Long userId);

    void deleteBySectionId(Long sectionId);
}
