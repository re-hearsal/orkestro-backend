package io.github.Romariok.orkestro.section.repository;

import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.models.SectionUserId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface SectionUserRepository extends JpaRepository<SectionUser, SectionUserId>,
        JpaSpecificationExecutor<SectionUser> {

    Optional<SectionUser> findBySectionIdAndUserId(Long sectionId, Long userId);

    List<SectionUser> findBySectionIdOrderByJoinedAtAsc(Long sectionId);

    long countBySectionId(Long sectionId);

    void deleteBySectionId(Long sectionId);

    void deleteBySectionIdInAndUserId(Collection<Long> sectionIds, Long userId);

    List<SectionUser> findBySectionIdIn(Collection<Long> sectionIds);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<SectionUser> findAll(Specification<SectionUser> spec, Pageable pageable);
}
