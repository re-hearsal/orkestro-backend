package io.github.Romariok.orkestro.task.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.task.dto.TaskAssigneesUpdateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.dto.TaskStatusUpdateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskUpdateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskVisibilityUpdateRequestDTO;
import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import io.github.Romariok.orkestro.task.service.TaskService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private TaskDTO buildTask(Long id, String title) {
        return TaskDTO.builder()
                .id(id)
                .organizationId(1L)
                .title(title)
                .status(TaskStatus.OPEN)
                .visibility(TaskVisibility.ALL_MEMBERS)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @Test
    void createTask_returnsCreated() {
        TaskDTO created = buildTask(10L, "Concert Prep");
        when(taskService.createTaskInOrganization(eq(1L), any(TaskCreateRequestDTO.class)))
                .thenReturn(created);
        TaskCreateRequestDTO request = TaskCreateRequestDTO.builder()
                .title("Concert Prep")
                .visibility(TaskVisibility.ALL_MEMBERS)
                .build();

        var result = taskController.createTask(1L, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
        assertEquals("Concert Prep", result.getBody().getTitle());
    }

    @Test
    void updateTask_returnsOk() {
        TaskDTO updated = buildTask(10L, "Updated Title");
        when(taskService.updateTask(eq(1L), eq(10L), any(TaskUpdateRequestDTO.class)))
                .thenReturn(updated);
        TaskUpdateRequestDTO request = TaskUpdateRequestDTO.builder()
                .title("Updated Title")
                .visibility(TaskVisibility.ALL_MEMBERS)
                .build();

        var result = taskController.updateTask(1L, 10L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Updated Title", result.getBody().getTitle());
    }

    @Test
    void updateTaskStatus_returnsOk() {
        TaskDTO updated = buildTask(10L, "Concert Prep");
        updated.setStatus(TaskStatus.IN_PROGRESS);
        when(taskService.updateTaskStatus(eq(1L), eq(10L), eq(TaskStatus.IN_PROGRESS)))
                .thenReturn(updated);
        TaskStatusUpdateRequestDTO request = new TaskStatusUpdateRequestDTO(TaskStatus.IN_PROGRESS);

        var result = taskController.updateTaskStatus(1L, 10L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(TaskStatus.IN_PROGRESS, result.getBody().getStatus());
    }

    @Test
    void updateTaskVisibility_returnsOk() {
        TaskDTO updated = buildTask(10L, "Concert Prep");
        updated.setVisibility(TaskVisibility.ROLE_RESTRICTED);
        when(taskService.updateTaskVisibility(eq(1L), eq(10L), any(TaskVisibilityUpdateRequestDTO.class)))
                .thenReturn(updated);
        TaskVisibilityUpdateRequestDTO request = new TaskVisibilityUpdateRequestDTO(TaskVisibility.ROLE_RESTRICTED, null);

        var result = taskController.updateTaskVisibility(1L, 10L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(TaskVisibility.ROLE_RESTRICTED, result.getBody().getVisibility());
    }

    @Test
    void addAssignees_returnsOk() {
        TaskDTO task = buildTask(10L, "Concert Prep");
        when(taskService.addAssignees(eq(1L), eq(10L), eq(List.of(5L))))
                .thenReturn(task);
        TaskAssigneesUpdateRequestDTO request = new TaskAssigneesUpdateRequestDTO(List.of(5L));

        var result = taskController.addAssignees(1L, 10L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
    }

    @Test
    void removeAssignee_returnsOk() {
        TaskDTO task = buildTask(10L, "Concert Prep");
        when(taskService.removeAssignee(1L, 10L, 5L)).thenReturn(task);

        var result = taskController.removeAssignee(1L, 10L, 5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(10L, result.getBody().getId());
    }

    @Test
    void getAvailableTasksPage_returnsOk() {
        Page<TaskDTO> page = new PageImpl<>(List.of(buildTask(1L, "Task A"), buildTask(2L, "Task B")));
        when(taskService.getAvailableTasksForCurrentUser(eq(1L), any())).thenReturn(page);

        var result = taskController.getAvailableTasksPage(1L, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(2, result.getBody().getTotalElements());
    }

    @Test
    void getClosedTasksPage_returnsOk() {
        Page<TaskDTO> page = new PageImpl<>(List.of(buildTask(3L, "Done Task")));
        when(taskService.getClosedTasksForCurrentUser(eq(1L), any())).thenReturn(page);

        var result = taskController.getClosedTasksPage(1L, PageRequest.of(0, 20));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().getTotalElements());
    }
}
