package io.github.Romariok.orkestro.task.service;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.task.dto.TaskCommentDTO;
import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.dto.TaskUpdateRequestDTO;
import io.github.Romariok.orkestro.task.mapper.TaskMapper;
import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.models.TaskComment;
import io.github.Romariok.orkestro.task.models.TaskFile;
import io.github.Romariok.orkestro.task.models.TaskVisibilityRole;
import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import io.github.Romariok.orkestro.task.repository.TaskCommentRepository;
import io.github.Romariok.orkestro.task.repository.TaskFileRepository;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.task.repository.TaskVisibilityRoleRepository;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.github.Romariok.orkestro.utils.helper.FileValidationHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskVisibilityRoleRepository taskVisibilityRoleRepository;
    private final StoredFileRepository storedFileRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final TaskMapper taskMapper;
    private final SecurityUtils securityUtils;

    /**
     * Создать задачу в организации.
     * Доступно только обладателям TASK_MANAGE в контексте организации.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'TASK_MANAGE')")
    public TaskDTO createTaskInOrganization(Long organizationId, TaskCreateRequestDTO request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Task title must not be blank");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

        Long authorUserId = securityUtils.getCurrentUserId();
        TaskVisibility visibility = request.getVisibility() != null ? request.getVisibility()
                : TaskVisibility.ALL_MEMBERS;

        if (request.getAssigneeUserId() != null) {
            validateAssigneeInOrganization(organization.getId(), request.getAssigneeUserId());
        }

        FileValidationHelper.validateFiles(request.getFileIds(), storedFileRepository);
        validateVisibilityRolesForOrganization(organization.getId(), visibility, request.getVisibilityRoleIds());

        Instant now = Instant.now();
        Task task = Task.builder()
                .organizationId(organization.getId())
                .sectionId(null)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .authorUserId(authorUserId)
                .assigneeUserId(request.getAssigneeUserId())
                .status(TaskStatus.OPEN)
                .visibility(visibility)
                .createdAt(now)
                .updatedAt(now)
                .closedAt(null)
                .build();

        Task saved = taskRepository.save(task);

        saveTaskFiles(saved.getId(), request.getFileIds());
        if (visibility == TaskVisibility.ROLE_RESTRICTED) {
            saveTaskVisibilityRoles(saved.getId(), request.getVisibilityRoleIds());
        }

        return buildTaskDto(saved);
    }

    /**
     * Обновить параметры задачи (кроме статуса).
     * Доступно только обладателям TASK_MANAGE в контексте организации задачи.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission("
            + "@taskRepository.findById(#taskId).orElse(null)?.organizationId, 'TASK_MANAGE')")
    public TaskDTO updateTask(Long taskId, TaskUpdateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Task title must not be blank");
            }
            task.setTitle(title);
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getAssigneeUserId() != null) {
            validateAssigneeInOrganization(task.getOrganizationId(), request.getAssigneeUserId());
            task.setAssigneeUserId(request.getAssigneeUserId());
        }

        TaskVisibility currentVisibility = task.getVisibility();
        TaskVisibility newVisibility = request.getVisibility() != null ? request.getVisibility() : currentVisibility;

        List<Long> requestedRoleIds = request.getVisibilityRoleIds();

        if (newVisibility == TaskVisibility.ROLE_RESTRICTED) {
            if (requestedRoleIds != null) {
                validateVisibilityRolesForOrganization(task.getOrganizationId(), newVisibility, requestedRoleIds);
                taskVisibilityRoleRepository.deleteByTaskId(taskId);
                saveTaskVisibilityRoles(taskId, requestedRoleIds);
            } else if (currentVisibility != TaskVisibility.ROLE_RESTRICTED) {
                throw new IllegalArgumentException(
                        "Visibility roles must be provided when changing to ROLE_RESTRICTED");
            }
        } else {
            if (currentVisibility == TaskVisibility.ROLE_RESTRICTED) {
                taskVisibilityRoleRepository.deleteByTaskId(taskId);
            }
        }

        task.setVisibility(newVisibility);

        if (request.getFileIds() != null) {
            FileValidationHelper.validateFiles(request.getFileIds(), storedFileRepository);
            taskFileRepository.deleteByTaskId(taskId);
            saveTaskFiles(taskId, request.getFileIds());
        }

        task.setUpdatedAt(Instant.now());
        Task saved = taskRepository.save(task);

        return buildTaskDto(saved);
    }

    /**
     * Изменить статус задачи.
     * Доступно только обладателям TASK_MANAGE в контексте организации задачи.
     */
    @Transactional
    @PreAuthorize("@organizationPermissionChecker.hasOrganizationPermission("
            + "@taskRepository.findById(#taskId).orElse(null)?.organizationId, 'TASK_MANAGE')")
    public TaskDTO updateTaskStatus(Long taskId, TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null");
        }

        Task task = taskRepository
                .findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));

        if (task.getStatus() == newStatus) {
            return buildTaskDto(task);
        }

        Instant now = Instant.now();
        task.setStatus(newStatus);
        task.setUpdatedAt(now);

        if (newStatus == TaskStatus.DONE || newStatus == TaskStatus.CANCELLED) {
            task.setClosedAt(now);
        } else {
            task.setClosedAt(null);
        }

        Task saved = taskRepository.save(task);
        return buildTaskDto(saved);
    }

    /**
     * Получить доступные пользователю задачи в организации (открытые и в работе).
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getAvailableTasksForCurrentUser(Long organizationId) {
        Long userId = securityUtils.getCurrentUserId();
        organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + organizationId));

        List<Task> tasks = taskRepository.findByOrganizationIdAndStatusIn(
                organizationId, List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS));

        if (tasks.isEmpty()) {
            return List.of();
        }

        return filterTasksByVisibilityAndMap(userId, tasks);
    }

    /**
     * Получить историю (закрытые задачи) для пользователя в организации.
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> getClosedTasksForCurrentUser(Long organizationId) {
        Long userId = securityUtils.getCurrentUserId();
        organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + organizationId));

        List<Task> tasks = taskRepository.findByOrganizationIdAndStatusIn(
                organizationId, List.of(TaskStatus.DONE, TaskStatus.CANCELLED));

        if (tasks.isEmpty()) {
            return List.of();
        }

        return filterTasksByVisibilityAndMap(userId, tasks);
    }

    private List<TaskDTO> filterTasksByVisibilityAndMap(Long userId, List<Task> tasks) {
        List<Role> userRoles = userRoleRepository.findRolesByUserId(userId);
        Set<Long> userRoleIds = userRoles.stream().map(Role::getId).collect(Collectors.toSet());

        List<Long> taskIds = tasks.stream().map(Task::getId).toList();

        Map<Long, List<Long>> taskRolesMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<TaskVisibilityRole> mappings = taskVisibilityRoleRepository.findByTaskIdIn(taskIds);
            for (TaskVisibilityRole mapping : mappings) {
                taskRolesMap
                        .computeIfAbsent(mapping.getTaskId(), id -> new ArrayList<>())
                        .add(mapping.getRoleId());
            }
        }

        List<TaskDTO> result = new ArrayList<>();
        for (Task task : tasks) {
            boolean isAuthorOrAssignee = (task.getAuthorUserId() != null && task.getAuthorUserId().equals(userId))
                    || (task.getAssigneeUserId() != null && task.getAssigneeUserId().equals(userId));

            if (isAuthorOrAssignee) {
                result.add(buildTaskDto(task));
                continue;
            }

            if (task.getVisibility() == TaskVisibility.ALL_MEMBERS) {
                result.add(buildTaskDto(task));
                continue;
            }

            List<Long> allowedRoleIds = taskRolesMap.get(task.getId());
            if (allowedRoleIds != null && !userRoleIds.isEmpty()) {
                boolean hasIntersection = allowedRoleIds.stream().anyMatch(userRoleIds::contains);
                if (hasIntersection) {
                    result.add(buildTaskDto(task));
                }
            }
        }
        return result;
    }

    private void validateAssigneeInOrganization(Long organizationId, Long assigneeUserId) {
        if (assigneeUserId == null) {
            return;
        }

        if (!userRepository.existsById(assigneeUserId)) {
            throw new EntityNotFoundException("User not found: " + assigneeUserId);
        }

        organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, assigneeUserId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + assigneeUserId + " is not an accepted member of organization " + organizationId));
    }

    private void validateVisibilityRolesForOrganization(
            Long organizationId, TaskVisibility visibility, List<Long> roleIds) {
        if (visibility != TaskVisibility.ROLE_RESTRICTED) {
            return;
        }

        if (roleIds == null || roleIds.isEmpty()) {
            throw new IllegalArgumentException("Visibility roles must not be empty when visibility is ROLE_RESTRICTED");
        }

        Set<Long> uniqueIds = new HashSet<>(roleIds);
        List<Role> roles = roleRepository.findAllById(uniqueIds);
        if (roles.size() != uniqueIds.size()) {
            throw new EntityNotFoundException("One or more roles not found for ids: " + uniqueIds);
        }

        for (Role role : roles) {
            if (role.getScope() != RoleScopeType.ORGANIZATION
                    || role.getOrganizationId() == null
                    || !role.getOrganizationId().equals(organizationId)) {
                throw new BusinessException(
                        "Role " + role.getId() + " does not belong to organization " + organizationId);
            }
        }
    }

    private void saveTaskFiles(Long taskId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<TaskFile> entities = fileIds.stream()
                .map(fileId -> {
                    TaskFile tf = new TaskFile();
                    tf.setTaskId(taskId);
                    tf.setFileId(fileId);
                    return tf;
                })
                .toList();

        taskFileRepository.saveAll(entities);
    }

    private void saveTaskVisibilityRoles(Long taskId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        List<TaskVisibilityRole> entities = roleIds.stream()
                .map(roleId -> {
                    TaskVisibilityRole tr = new TaskVisibilityRole();
                    tr.setTaskId(taskId);
                    tr.setRoleId(roleId);
                    return tr;
                })
                .toList();

        taskVisibilityRoleRepository.saveAll(entities);
    }

    private TaskDTO buildTaskDto(Task task) {
        TaskDTO dto = taskMapper.toDto(task);

        List<TaskFile> files = taskFileRepository.findByTaskId(task.getId());
        List<Long> fileIds = files.stream().map(TaskFile::getFileId).toList();

        List<TaskComment> comments = taskCommentRepository.findByTaskId(task.getId());
        List<TaskCommentDTO> commentDtos = comments.stream()
                .map(this::mapCommentToDto)
                .toList();

        List<TaskVisibilityRole> visibilityRoles = taskVisibilityRoleRepository.findByTaskId(task.getId());
        List<Long> roleIds = visibilityRoles.stream().map(TaskVisibilityRole::getRoleId).toList();

        dto.setFileIds(fileIds);
        dto.setComments(commentDtos);
        dto.setVisibilityRoleIds(roleIds);
        return dto;
    }

    private TaskCommentDTO mapCommentToDto(TaskComment comment) {
        TaskCommentDTO dto = new TaskCommentDTO();
        dto.setId(comment.getId());
        dto.setTaskId(comment.getTaskId());
        dto.setAuthorUserId(comment.getAuthorUserId());
        dto.setText(comment.getText());
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}
