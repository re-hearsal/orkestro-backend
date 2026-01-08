package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.section.Section;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByParentSectionId(Long parentSectionId);

    boolean existsByOrganizationIdAndParentSectionIdIsNullAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndParentSectionIdAndName(Long organizationId, Long parentSectionId, String name);
}
