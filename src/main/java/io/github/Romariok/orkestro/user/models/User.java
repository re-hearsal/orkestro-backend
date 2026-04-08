package io.github.Romariok.orkestro.user.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(name = "username", nullable = false)
   private String username;

   @Column(name = "name", nullable = false)
   private String name;

   @Column(name = "email", nullable = false)
   private String email;

   @Column(name = "password", nullable = false)
   private String password;

   @CreationTimestamp
   @Column(name = "created_at", nullable = false)
   private Instant createdAt;

   @UpdateTimestamp
   @Column(name = "updated_at", nullable = false)
   private Instant updatedAt;

   @Column(name = "telegram_user_id")
   private Long telegramUserId;

   @Column(name = "vk_user_id")
   private Long vkUserId;

   @Enumerated(EnumType.STRING)
   @JdbcTypeCode(SqlTypes.NAMED_ENUM)
   @ColumnDefault("'EMAIL'")
   @Column(name = "notification_channel_id", nullable = false)
   private NotificationChannelType notificationChannel;

   @Enumerated(EnumType.STRING)
   @JdbcTypeCode(SqlTypes.NAMED_ENUM)
   @ColumnDefault("'RU'")
   @Column(name = "preferred_language", nullable = false)
   private UserLanguageType preferredLanguage;

   @Column(name = "location")
   private String location;

   @Column(name = "birth_date")
   private LocalDate birthDate;

   @Column(name = "profile_image_file_id")
   private Long profileImageFileId;

   @Override
   public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of(new SimpleGrantedAuthority("USER"));
   }

   @Override
   public boolean isAccountNonExpired() {
      return true;
   }

   @Override
   public boolean isAccountNonLocked() {
      return true;
   }

   @Override
   public boolean isCredentialsNonExpired() {
      return true;
   }

   @Override
   public boolean isEnabled() {
      return true;
   }
}
