package io.github.Romariok.orkestro.task.controller;

import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.dto.TaskFileAttachRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskVisibilityUpdateRequestDTO;
import io.github.Romariok.orkestro.task.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/organizations/{organizationId}/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskDTO> createTask(
            @PathVariable @Positive Long organizationId,
            @Valid @ModelAttribute TaskCreateRequestDTO request) {
        TaskDTO created = taskService.createTaskInOrganization(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{taskId}/visibility")
    public ResponseEntity<TaskDTO> updateTaskVisibility(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long taskId,
            @Valid @RequestBody TaskVisibilityUpdateRequestDTO request) {
        return ResponseEntity.ok(taskService.updateTaskVisibility(organizationId, taskId, request));
    }

    @GetMapping("/closed/page")
    public ResponseEntity<Page<TaskDTO>> getClosedTasksPage(
            @PathVariable @Positive Long organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getClosedTasksForCurrentUser(organizationId, pageable));
    }

    @PostMapping(value = "/{taskId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TaskDTO> attachFileToTask(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long taskId,
            @Valid @ModelAttribute TaskFileAttachRequestDTO request) {
        TaskDTO updated = taskService.attachFileToTaskForCurrentUser(organizationId, taskId, request.getFile());
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    @DeleteMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<TaskDTO> deleteFileFromTask(
            @PathVariable @Positive Long organizationId,
            @PathVariable @Positive Long taskId,
            @PathVariable @Positive Long fileId) {
        return ResponseEntity.ok(taskService.deleteTaskFileForCurrentUser(organizationId, taskId, fileId));
    }

    @GetMapping("/available/page")
    public ResponseEntity<Page<TaskDTO>> getAvailableTasksPage(
            @PathVariable @Positive Long organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getAvailableTasksForCurrentUser(organizationId, pageable));
    }
}
