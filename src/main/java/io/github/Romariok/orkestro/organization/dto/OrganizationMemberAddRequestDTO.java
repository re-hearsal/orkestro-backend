package io.github.Romariok.orkestro.organization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberAddRequestDTO {

   @NotNull
   @Positive
   private Long userId;
}

