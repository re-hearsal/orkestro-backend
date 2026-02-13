package io.github.Romariok.orkestro.event.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSearchRequestDTO {
    private String title;
    private List<String> tags;
    private Instant from;
    private Instant to;
}
