package io.github.Romariok.orkestro.user.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.user.models.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    List<Permission> findByCodeIn(Collection<String> codes);
}
