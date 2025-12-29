package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByNameContainingIgnoreCase(String name);

    @Query("""
            SELECT DISTINCT u
            FROM User u
            JOIN UserRole ur ON ur.userId = u.id
            WHERE (:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND ur.roleId IN :roleIds
            """)
    List<User> findByNameAndRoleIds(
            @Param("name") String name,
            @Param("roleIds") List<Long> roleIds);
}