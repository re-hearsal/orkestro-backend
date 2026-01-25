package io.github.Romariok.orkestro.repertoire.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Builder
@AllArgsConstructor
@Entity
@Table(name = "song")
public class Song {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "organization_id", nullable = false)
   private Long organizationId;

   @Column(name = "title", nullable = false)
   private String title;

   @Column(name = "composer")
   private String composer;

   @Column(name = "duration_seconds")
   private Integer durationSeconds;

   @Column(name = "description")
   private String description;

   @Column(name = "video_url")
   private String videoUrl;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false)
   private Instant createdAt;
}
