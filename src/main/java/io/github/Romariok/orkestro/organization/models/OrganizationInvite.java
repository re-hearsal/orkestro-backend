package io.github.Romariok.orkestro.organization.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "organization_invite")
public class OrganizationInvite {

   @Id
   @Column(name = "organization_id")
   private Long organizationId;

   @Column(name = "code", nullable = false, unique = true)
   private String code;

   @Column(name = "created_by_user_id", nullable = false)
   private Long createdByUserId;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false)
   private Instant createdAt;
}
