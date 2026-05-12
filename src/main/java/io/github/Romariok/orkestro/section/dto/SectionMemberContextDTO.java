package io.github.Romariok.orkestro.section.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionMemberContextDTO {

    private boolean member;
    private RoleInfo role;
    private List<String> permissions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleInfo {
        private Long id;
        private String name;
    }
}
