package io.github.Romariok.orkestro.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.Romariok.orkestro.models.role.Permission;
import io.github.Romariok.orkestro.models.role.RolePermission;
import io.github.Romariok.orkestro.models.role.RolePermissionId;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

   @Query("SELECT p FROM Permission p JOIN RolePermission rp ON rp.permissionCode = p.code WHERE rp.roleId = :roleId")
   List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);
}
