package io.github.Romariok.orkestro.models.organization;

import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "organization_users")
@IdClass(OrganizationUserId.class)
public class OrganizationUser {

   @Id
   @Column(name = "organization_id", nullable = false)
   private Long organizationId;

   @Id
   @Column(name = "user_id", nullable = false)
   private Long userId;

   @Enumerated(EnumType.STRING)
   @Column(name = "status", nullable = false)
   private OrganizationUserStatusType status;

   @Column(name = "joined_at", nullable = false)
   private Instant joinedAt;
}
