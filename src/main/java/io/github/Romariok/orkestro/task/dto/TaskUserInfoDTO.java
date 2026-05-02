package io.github.Romariok.orkestro.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskUserInfoDTO {

    private Long userId;
    private String username;
    private String name;
    private Long profileImageFileId;
}
