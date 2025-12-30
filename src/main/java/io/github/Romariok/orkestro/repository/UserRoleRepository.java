package io.github.Romariok.orkestro.repository;

import io.github.Romariok.orkestro.models.Role;
import io.github.Romariok.orkestro.models.UserRole;
import io.github.Romariok.orkestro.models.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

   @Query("SELECT r FROM Role r JOIN UserRole ur ON ur.roleId = r.id WHERE ur.userId = :userId")
   List<Role> findRolesByUserId(@Param("userId") Long userId);

   void deleteByUserId(Long userId);
}
