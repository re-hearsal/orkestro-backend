package io.github.Romariok.orkestro.section.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.section.models.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByOrganizationId(Long organizationId);

    List<Section> findByParentSectionId(Long parentSectionId);

    boolean existsByOrganizationIdAndParentSectionIdIsNullAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndParentSectionIdAndName(Long organizationId, Long parentSectionId, String name);
}
