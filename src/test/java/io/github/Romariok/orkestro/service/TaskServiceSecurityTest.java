package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskUpdateRequestDTO;
import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import io.github.Romariok.orkestro.task.service.TaskService;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class TaskServiceSecurityTest {

    @Test
    void createTaskInOrganization_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = TaskService.class.getMethod("createTaskInOrganization", Long.class, TaskCreateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals("hasAuthority('CTX_PERM_ORG:' + #organizationId + ':TASK_MANAGE')", expr);
    }

    @Test
    void updateTask_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = TaskService.class.getMethod("updateTask", Long.class, TaskUpdateRequestDTO.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "hasAuthority('CTX_PERM_ORG:' + "
                        + "@taskRepository.findById(#taskId).orElse(null)?.organizationId + ':TASK_MANAGE')",
                expr);
    }

    @Test
    void updateTaskStatus_hasExpectedPreAuthorizeExpression() throws NoSuchMethodException {
        Method method = TaskService.class.getMethod("updateTaskStatus", Long.class, TaskStatus.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        String expr = preAuthorize.value();
        assertEquals(
                "hasAuthority('CTX_PERM_ORG:' + "
                        + "@taskRepository.findById(#taskId).orElse(null)?.organizationId + ':TASK_MANAGE')",
                expr);
    }
}
