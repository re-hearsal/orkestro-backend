package io.github.Romariok.orkestro.organization.models;

import io.github.Romariok.orkestro.organization.models.enums.LinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
@Table(name = "organization_links")
public class OrganizationLink {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "organization_id", nullable = false)
   private Long organizationId;

   @Enumerated(EnumType.STRING)
   @JdbcTypeCode(SqlTypes.NAMED_ENUM)
   @Column(name = "link_type", nullable = false)
   private LinkType linkType;

   @Column(name = "url", nullable = false)
   private String url;
}
