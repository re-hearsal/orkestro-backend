package io.github.Romariok.orkestro.models.organization;

import io.github.Romariok.orkestro.models.enums.LinkType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_links")
public class OrganizationLink {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "organization_id", nullable = false)
   private Long organizationId;

   @Enumerated(EnumType.STRING)
   @Column(name = "link_type", nullable = false)
   private LinkType linkType;

   @Column(name = "url", nullable = false)
   private String url;
}
