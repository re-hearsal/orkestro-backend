package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.dto.task.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.dto.task.TaskDTO;
import io.github.Romariok.orkestro.mapper.TaskMapper;
import io.github.Romariok.orkestro.models.StoredFile;
import io.github.Romariok.orkestro.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.models.enums.TaskStatus;
import io.github.Romariok.orkestro.models.enums.TaskVisibility;
import io.github.Romariok.orkestro.models.organization.Organization;
import io.github.Romariok.orkestro.models.organization.OrganizationUser;
import io.github.Romariok.orkestro.models.role.Role;
import io.github.Romariok.orkestro.models.task.Task;
import io.github.Romariok.orkestro.models.task.TaskVisibilityRole;
import io.github.Romariok.orkestro.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repository.RoleRepository;
import io.github.Romariok.orkestro.repository.StoredFileRepository;
import io.github.Romariok.orkestro.repository.TaskCommentRepository;
import io.github.Romariok.orkestro.repository.TaskFileRepository;
import io.github.Romariok.orkestro.repository.TaskRepository;
import io.github.Romariok.orkestro.repository.TaskVisibilityRoleRepository;
import io.github.Romariok.orkestro.repository.UserRepository;
import io.github.Romariok.orkestro.repository.UserRoleRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

        @Mock
        private TaskRepository taskRepository;

        @Mock
        private TaskFileRepository taskFileRepository;

        @Mock
        private TaskCommentRepository taskCommentRepository;

        @Mock
        private TaskVisibilityRoleRepository taskVisibilityRoleRepository;

        @Mock
        private StoredFileRepository storedFileRepository;

        @Mock
        private RoleRepository roleRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private UserRoleRepository userRoleRepository;

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private OrganizationUserRepository organizationUserRepository;

        @Mock
        private TaskMapper taskMapper;

        @Mock
        private SecurityUtils securityUtils;

        @InjectMocks
        private TaskService taskService;

        @Test
        void createTaskInOrganization_success_savesTask() {
                Long organizationId = 1L;
                Long currentUserId = 10L;
                Long assigneeId = 20L;

                TaskCreateRequestDTO request = new TaskCreateRequestDTO(
                                "Title", "Desc", assigneeId, TaskVisibility.ALL_MEMBERS, null, null);

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Org")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                when(userRepository.existsById(assigneeId)).thenReturn(true);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(assigneeId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, assigneeId))
                                .thenReturn(Optional.of(membership));

                Task saved = Task.builder()
                                .id(100L)
                                .organizationId(organizationId)
                                .title("Title")
                                .description("Desc")
                                .authorUserId(currentUserId)
                                .assigneeUserId(assigneeId)
                                .status(TaskStatus.OPEN)
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                when(taskRepository.save(any(Task.class))).thenReturn(saved);

                when(taskMapper.toDto(any(Task.class))).thenAnswer(invocation -> {
                        Task task = invocation.getArgument(0);
                        TaskDTO dto = new TaskDTO();
                        dto.setId(task.getId());
                        dto.setOrganizationId(task.getOrganizationId());
                        dto.setSectionId(task.getSectionId());
                        dto.setTitle(task.getTitle());
                        dto.setDescription(task.getDescription());
                        dto.setAuthorUserId(task.getAuthorUserId());
                        dto.setAssigneeUserId(task.getAssigneeUserId());
                        dto.setStatus(task.getStatus());
                        dto.setVisibility(task.getVisibility());
                        dto.setCreatedAt(task.getCreatedAt());
                        dto.setUpdatedAt(task.getUpdatedAt());
                        dto.setClosedAt(task.getClosedAt());
                        return dto;
                });

                when(taskFileRepository.findByTaskId(anyLong())).thenReturn(List.of());
                when(taskCommentRepository.findByTaskId(anyLong())).thenReturn(List.of());
                when(taskVisibilityRoleRepository.findByTaskId(anyLong())).thenReturn(List.of());

                TaskDTO result = taskService.createTaskInOrganization(organizationId, request);

                ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
                verify(taskRepository).save(captor.capture());
                Task persisted = captor.getValue();

                assertEquals(organizationId, persisted.getOrganizationId());
                assertEquals("Title", persisted.getTitle());
                assertEquals("Desc", persisted.getDescription());
                assertEquals(currentUserId, persisted.getAuthorUserId());
                assertEquals(assigneeId, persisted.getAssigneeUserId());

                assertEquals("Title", result.getTitle());
                assertEquals(currentUserId, result.getAuthorUserId());
        }

        @Test
        void createTaskInOrganization_blankTitle_throwsIllegalArgumentException() {
                Long organizationId = 1L;
                TaskCreateRequestDTO request = new TaskCreateRequestDTO("   ", "Desc", null, null, null, null);

                assertThrows(
                                IllegalArgumentException.class,
                                () -> taskService.createTaskInOrganization(organizationId, request));

                verify(organizationRepository, never()).findById(anyLong());
                verify(taskRepository, never()).save(any());
        }

        @Test
        void updateTaskStatus_setsClosedAtForDone() {
                Long taskId = 100L;

                Task existing = Task.builder()
                                .id(taskId)
                                .organizationId(1L)
                                .title("Title")
                                .status(TaskStatus.OPEN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                when(taskRepository.findById(taskId)).thenReturn(Optional.of(existing));

                when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

                when(taskMapper.toDto(any(Task.class))).thenAnswer(invocation -> {
                        Task task = invocation.getArgument(0);
                        TaskDTO dto = new TaskDTO();
                        dto.setId(task.getId());
                        dto.setOrganizationId(task.getOrganizationId());
                        dto.setStatus(task.getStatus());
                        dto.setClosedAt(task.getClosedAt());
                        return dto;
                });

                when(taskFileRepository.findByTaskId(anyLong())).thenReturn(List.of());
                when(taskCommentRepository.findByTaskId(anyLong())).thenReturn(List.of());
                when(taskVisibilityRoleRepository.findByTaskId(anyLong())).thenReturn(List.of());

                TaskDTO result = taskService.updateTaskStatus(taskId, TaskStatus.DONE);

                assertEquals(TaskStatus.DONE, result.getStatus());
                verify(taskRepository).save(existing);
        }

        @Test
        void getAvailableTasksForCurrentUser_filtersByVisibilityRolesAndAuthorAssignee() {
                Long organizationId = 1L;
                Long userId = 10L;

                when(securityUtils.getCurrentUserId()).thenReturn(userId);

                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                Task taskAll = Task.builder()
                                .id(1L)
                                .organizationId(organizationId)
                                .title("All")
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Task taskRestrictedByRole = Task.builder()
                                .id(2L)
                                .organizationId(organizationId)
                                .title("RestrictedByRole")
                                .visibility(TaskVisibility.ROLE_RESTRICTED)
                                .status(TaskStatus.OPEN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                Task taskRestrictedByAuthor = Task.builder()
                                .id(3L)
                                .organizationId(organizationId)
                                .title("RestrictedByAuthor")
                                .visibility(TaskVisibility.ROLE_RESTRICTED)
                                .status(TaskStatus.OPEN)
                                .authorUserId(userId)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();

                when(taskRepository.findByOrganizationIdAndStatusIn(
                                organizationId, List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS)))
                                .thenReturn(List.of(taskAll, taskRestrictedByRole, taskRestrictedByAuthor));

                Role role = Role.builder().id(100L).build();
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of(role));

                TaskVisibilityRole mapping = new TaskVisibilityRole();
                mapping.setTaskId(2L);
                mapping.setRoleId(100L);
                when(taskVisibilityRoleRepository.findByTaskIdIn(List.of(1L, 2L, 3L)))
                                .thenReturn(List.of(mapping));

                when(taskMapper.toDto(any(Task.class))).thenAnswer(invocation -> {
                        Task task = invocation.getArgument(0);
                        return TaskDTO.builder()
                                        .id(task.getId())
                                        .organizationId(task.getOrganizationId())
                                        .title(task.getTitle())
                                        .visibility(task.getVisibility())
                                        .status(task.getStatus())
                                        .createdAt(task.getCreatedAt())
                                        .updatedAt(task.getUpdatedAt())
                                        .build();
                });

                when(taskFileRepository.findByTaskId(anyLong())).thenReturn(Collections.emptyList());
                when(taskCommentRepository.findByTaskId(anyLong())).thenReturn(Collections.emptyList());
                when(taskVisibilityRoleRepository.findByTaskId(anyLong())).thenReturn(Collections.emptyList());

                List<TaskDTO> result = taskService.getAvailableTasksForCurrentUser(organizationId);

                assertEquals(3, result.size());
        }

        @Test
        void getAvailableTasksForCurrentUser_userNotMember_throwsBusinessException() {
                Long organizationId = 1L;
                Long userId = 10L;

                when(securityUtils.getCurrentUserId()).thenReturn(userId);

                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                BusinessException.class,
                                () -> taskService.getAvailableTasksForCurrentUser(organizationId));

                verify(taskRepository, never()).findByOrganizationIdAndStatusIn(anyLong(), any());
        }

        @Test
        void createTaskInOrganization_withNonExistingFile_throwsEntityNotFound() {
                Long organizationId = 1L;
                Long currentUserId = 10L;

                TaskCreateRequestDTO request = new TaskCreateRequestDTO(
                                "Title", "Desc", null, TaskVisibility.ALL_MEMBERS, null, List.of(1L, 2L));

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Org")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                when(storedFileRepository.findAllById(any()))
                                .thenReturn(List.of(StoredFile.builder().id(1L).build()));

                assertThrows(
                                EntityNotFoundException.class,
                                () -> taskService.createTaskInOrganization(organizationId, request));

                verify(taskRepository, never()).save(any());
        }
}
