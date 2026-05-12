package io.github.Romariok.orkestro.user.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "users_instrument")
@IdClass(UserInstrumentId.class)
public class UserInstrument {

   @Id
   @Column(name = "user_id", nullable = false)
   private Long userId;

   @Id
   @Column(name = "instrument_id", nullable = false)
   private Long instrumentId;
}
