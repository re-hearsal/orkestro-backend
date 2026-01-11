package io.github.Romariok.orkestro.section.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.models.SectionUserId;

@Repository
public interface SectionUserRepository extends JpaRepository<SectionUser, SectionUserId> {

    Optional<SectionUser> findBySectionIdAndUserId(Long sectionId, Long userId);

    void deleteBySectionId(Long sectionId);
}
