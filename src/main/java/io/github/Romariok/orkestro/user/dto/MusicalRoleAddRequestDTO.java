package io.github.Romariok.orkestro.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicalRoleAddRequestDTO {

   @NotEmpty
   private List<@Positive Long> instrumentIds;
}

