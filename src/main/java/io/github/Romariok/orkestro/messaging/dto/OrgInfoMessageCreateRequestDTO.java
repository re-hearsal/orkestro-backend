package io.github.Romariok.orkestro.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrgInfoMessageCreateRequestDTO {

    @NotBlank
    @Size(max = 5000)
    private String text;
}
