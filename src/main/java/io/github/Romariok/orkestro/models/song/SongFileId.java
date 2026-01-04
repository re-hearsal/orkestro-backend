package io.github.Romariok.orkestro.models.song;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SongFileId implements Serializable {

   private static final long serialVersionUID = 1L;

   private Long songId;
   private Long fileId;
}
