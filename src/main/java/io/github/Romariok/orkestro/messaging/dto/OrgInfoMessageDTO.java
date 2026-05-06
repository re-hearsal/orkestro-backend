package io.github.Romariok.orkestro.messaging.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class OrgInfoMessageDTO {

    private Long id;
    private Long organizationId;
    private Long sectionId;
    private Long authorUserId;
    private String authorName;
    private Long authorProfileImageFileId;
    private String text;
    private Instant createdAt;
}
