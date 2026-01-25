package io.github.Romariok.orkestro.user.models;

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
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_telegram_link_token")
public class UserTelegramLinkToken {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "user_id", nullable = false)
   private Long userId;

   @Column(name = "token", nullable = false, unique = true)
   private String token;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @Column(name = "expires_at")
   private Instant expiresAt;

   @Column(name = "used_at")
   private Instant usedAt;
}
