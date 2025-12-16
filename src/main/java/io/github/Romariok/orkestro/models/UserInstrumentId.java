package io.github.Romariok.orkestro.models;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class UserInstrumentId implements Serializable {

   private Long userId;
   private Long instrumentId;
}
