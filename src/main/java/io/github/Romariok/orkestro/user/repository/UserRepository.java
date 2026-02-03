package io.github.Romariok.orkestro.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  Optional<User> findByTelegramUserId(Long telegramUserId);

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

  @Query(value = """
      SELECT DISTINCT u
      FROM User u
      JOIN OrganizationUser ou ON ou.userId = u.id
      LEFT JOIN UserRole ur ON ur.userId = u.id
      LEFT JOIN Role r ON r.id = ur.roleId
        AND r.scope = :roleScope
        AND r.organizationId = :organizationId
      LEFT JOIN UserInstrument ui ON ui.userId = u.id
      WHERE ou.organizationId = :organizationId
        AND ou.status = :membershipStatus
        AND (
          :query IS NULL
          OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:applyRoleFilter = false OR r.id IN :roleIds)
        AND (:applyInstrumentFilter = false OR ui.instrumentId IN :instrumentIds)
      """, countQuery = """
      SELECT COUNT(DISTINCT u.id)
      FROM User u
      JOIN OrganizationUser ou ON ou.userId = u.id
      LEFT JOIN UserRole ur ON ur.userId = u.id
      LEFT JOIN Role r ON r.id = ur.roleId
        AND r.scope = :roleScope
        AND r.organizationId = :organizationId
      LEFT JOIN UserInstrument ui ON ui.userId = u.id
      WHERE ou.organizationId = :organizationId
        AND ou.status = :membershipStatus
        AND (
          :query IS NULL
          OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:applyRoleFilter = false OR r.id IN :roleIds)
        AND (:applyInstrumentFilter = false OR ui.instrumentId IN :instrumentIds)
      """)
  Page<User> searchOrganizationMembers(
      @Param("organizationId") Long organizationId,
      @Param("query") String query,
      @Param("applyRoleFilter") boolean applyRoleFilter,
      @Param("roleIds") List<Long> roleIds,
      @Param("applyInstrumentFilter") boolean applyInstrumentFilter,
      @Param("instrumentIds") List<Long> instrumentIds,
      @Param("membershipStatus") OrganizationUserStatusType membershipStatus,
      @Param("roleScope") RoleScopeType roleScope,
      Pageable pageable);
}