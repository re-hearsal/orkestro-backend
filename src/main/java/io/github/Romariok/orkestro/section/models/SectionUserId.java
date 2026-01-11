package io.github.Romariok.orkestro.section.models;

import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class SectionUserId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sectionId;
    private Long userId;
}


