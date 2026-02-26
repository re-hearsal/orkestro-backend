package io.github.Romariok.orkestro.repertoire.models;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
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

   @Column(name = "description", length = 3000)
   private String description;

   @Column(name = "video_url")
   private String videoUrl;

   @ElementCollection
   @CollectionTable(name = "song_tags", joinColumns = @JoinColumn(name = "song_id"))
   @Column(name = "tag", nullable = false)
   @Builder.Default
   private Set<String> tags = new HashSet<>();

   @CreationTimestamp
   @Column(name = "created_at", nullable = false)
   private Instant createdAt;
}
