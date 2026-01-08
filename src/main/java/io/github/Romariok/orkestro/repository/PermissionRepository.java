package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.role.Permission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    List<Permission> findByCodeIn(Collection<String> codes);
}
