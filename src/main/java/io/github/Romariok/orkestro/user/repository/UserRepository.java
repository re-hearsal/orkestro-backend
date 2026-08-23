package io.github.Romariok.orkestro.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.user.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
  boolean existsByProfileImageFileId(Long profileImageFileId);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  Optional<User> findByTelegramUserId(Long telegramUserId);

  Optional<User> findByVkUserId(Long vkUserId);

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