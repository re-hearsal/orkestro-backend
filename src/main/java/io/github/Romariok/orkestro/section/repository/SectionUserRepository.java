package io.github.Romariok.orkestro.section.repository;

import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.models.SectionUserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionUserRepository extends JpaRepository<SectionUser, SectionUserId> {

    Optional<SectionUser> findBySectionIdAndUserId(Long sectionId, Long userId);

    void deleteBySectionId(Long sectionId);

    List<SectionUser> findBySectionIdIn(Collection<Long> sectionIds);
}
