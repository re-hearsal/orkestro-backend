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
public class TaskFileId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long fileId;
}


