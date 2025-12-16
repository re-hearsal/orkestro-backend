package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.Permission;
import io.github.Romariok.orkestro.models.RolePermission;
import io.github.Romariok.orkestro.models.RolePermissionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

   @Query("SELECT p FROM Permission p JOIN RolePermission rp ON rp.permissionCode = p.code WHERE rp.roleId = :roleId")
   List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);
}
