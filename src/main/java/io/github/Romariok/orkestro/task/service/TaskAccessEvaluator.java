package io.github.Romariok.orkestro.task.service;

import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaskAccessEvaluator {

    public boolean hasTaskAccess(
            Long userId, Task task, Set<Long> userRoleIds, Map<Long, List<Long>> taskRolesMap) {
        boolean isAuthorOrAssignee = (task.getAuthorUserId() != null && task.getAuthorUserId().equals(userId))
                || (task.getAssigneeUserId() != null && task.getAssigneeUserId().equals(userId));
        if (isAuthorOrAssignee || task.getVisibility() == TaskVisibility.ALL_MEMBERS) {
            return true;
        }

        List<Long> allowedRoleIds = taskRolesMap.get(task.getId());
        return allowedRoleIds != null
                && !userRoleIds.isEmpty()
                && allowedRoleIds.stream().anyMatch(userRoleIds::contains);
    }
}
