package io.github.Romariok.orkestro.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCommentCreateRequestDTO {

    @NotBlank
    @Size(max = 3000)
    private String text;

    @Min(0)
    @Max(10)
    private Integer rating;
}
