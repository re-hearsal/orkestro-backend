package io.github.Romariok.orkestro.organization.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.user.models.User;
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

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
   private User user;

   @Enumerated(EnumType.STRING)
   @JdbcTypeCode(SqlTypes.NAMED_ENUM)
   @Column(name = "status", nullable = false)
   private OrganizationUserStatusType status;

   @CreationTimestamp
   @Column(name = "joined_at", nullable = false)
   private Instant joinedAt;
}
