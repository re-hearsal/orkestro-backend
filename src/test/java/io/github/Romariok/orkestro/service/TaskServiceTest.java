package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.task.dto.TaskCreateRequestDTO;
import io.github.Romariok.orkestro.task.dto.TaskDTO;
import io.github.Romariok.orkestro.task.mapper.TaskMapper;
import io.github.Romariok.orkestro.task.models.Task;
import io.github.Romariok.orkestro.task.models.TaskVisibilityRole;
import io.github.Romariok.orkestro.task.models.enums.TaskStatus;
import io.github.Romariok.orkestro.task.models.enums.TaskVisibility;
import io.github.Romariok.orkestro.task.repository.TaskCommentRepository;
import io.github.Romariok.orkestro.task.repository.TaskFileRepository;
import io.github.Romariok.orkestro.task.repository.TaskRepository;
import io.github.Romariok.orkestro.task.repository.TaskVisibilityRoleRepository;
import io.github.Romariok.orkestro.task.service.TaskService;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

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

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private FileReferenceService fileReferenceService;

        @Mock
        private FileRollbackHelper fileRollbackHelper;

        @Mock
        private FileLimitsProperties fileLimitsProperties;

        @InjectMocks
        private TaskService taskService;

        @BeforeEach
        void setup() {
                lenient().when(fileLimitsProperties.getTaskMaxFiles()).thenReturn(50);
        }

        @Test
        void createTaskInOrganization_success_uploadsAndAttachesFiles() {
                Long organizationId = 1L;
                Long currentUserId = 10L;
                Long assigneeId = 20L;

                MockMultipartFile first = new MockMultipartFile(
                                "files", "task-note.pdf", "application/pdf", "pdf".getBytes());
                MockMultipartFile second = new MockMultipartFile(
                                "files", "task-audio.mp3", "audio/mpeg", "audio".getBytes());

                TaskCreateRequestDTO request = new TaskCreateRequestDTO(
                                "Title", "Desc", assigneeId, TaskVisibility.ALL_MEMBERS, null, List.of(first, second));

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Org")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(fileStorageService.uploadForCurrentUser(any(), any()))
                                .thenReturn(StoredFile.builder().id(501L).build())
                                .thenReturn(StoredFile.builder().id(502L).build());

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
                verify(taskFileRepository).saveAll(any());
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
                PageRequest pageable = PageRequest.of(0, 20);

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
                                organizationId, List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS), pageable))
                                .thenReturn(new PageImpl<>(
                                                List.of(taskAll, taskRestrictedByRole, taskRestrictedByAuthor), pageable, 3));

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

                Page<TaskDTO> result = taskService.getAvailableTasksForCurrentUser(organizationId, pageable);

                assertEquals(3, result.getContent().size());
        }

        @Test
        void getAvailableTasksForCurrentUser_userNotMember_throwsBusinessException() {
                Long organizationId = 1L;
                Long userId = 10L;
                PageRequest pageable = PageRequest.of(0, 20);

                when(securityUtils.getCurrentUserId()).thenReturn(userId);

                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                BusinessException.class,
                                () -> taskService.getAvailableTasksForCurrentUser(organizationId, pageable));

                verify(taskRepository, never()).findByOrganizationIdAndStatusIn(anyLong(), any(), any());
        }

        @Test
        void attachFileToTaskForCurrentUser_success_uploadsAndAttaches() {
                Long organizationId = 1L;
                Long taskId = 200L;
                Long userId = 10L;
                Long uploadedFileId = 777L;

                Task task = Task.builder()
                                .id(taskId)
                                .organizationId(organizationId)
                                .title("Task")
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .authorUserId(99L)
                                .assigneeUserId(98L)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                MockMultipartFile multipartFile = new MockMultipartFile(
                                "file", "attach.pdf", "application/pdf", "pdf-content".getBytes());

                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));
                when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());
                when(fileStorageService.uploadForCurrentUser(any(), any()))
                                .thenReturn(StoredFile.builder().id(uploadedFileId).build());
                when(taskFileRepository.existsByTaskIdAndFileId(taskId, uploadedFileId)).thenReturn(false);
                when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

                when(taskMapper.toDto(any(Task.class))).thenAnswer(invocation -> {
                        Task mapped = invocation.getArgument(0);
                        TaskDTO dto = new TaskDTO();
                        dto.setId(mapped.getId());
                        dto.setOrganizationId(mapped.getOrganizationId());
                        dto.setTitle(mapped.getTitle());
                        dto.setVisibility(mapped.getVisibility());
                        dto.setStatus(mapped.getStatus());
                        return dto;
                });
                when(taskFileRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(taskCommentRepository.findByTaskId(taskId)).thenReturn(List.of());

                TaskDTO result = taskService.attachFileToTaskForCurrentUser(organizationId, taskId, multipartFile);

                assertEquals(taskId, result.getId());
                verify(taskFileRepository).save(any());
                verify(fileStorageService).uploadForCurrentUser(any(), any());
        }

        @Test
        void createTaskInOrganization_rollsBackUploadedFiles_whenSaveFails() {
                Long organizationId = 1L;
                Long currentUserId = 10L;
                MockMultipartFile first = new MockMultipartFile(
                                "files", "task-note.pdf", "application/pdf", "pdf".getBytes());

                TaskCreateRequestDTO request = new TaskCreateRequestDTO(
                                "Title", "Desc", null, TaskVisibility.ALL_MEMBERS, null, List.of(first));
                Organization organization = Organization.builder().id(organizationId).name("Org").location("City").build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(fileStorageService.uploadForCurrentUser(any(), any()))
                                .thenReturn(StoredFile.builder().id(901L).build());
                doThrow(new RuntimeException("db error")).when(taskRepository).save(any(Task.class));

                assertThrows(
                                RuntimeException.class,
                                () -> taskService.createTaskInOrganization(organizationId, request));

                verify(fileRollbackHelper).deleteFilesSafely(List.of(901L));
        }

        @Test
        void attachFileToTaskForCurrentUser_rollsBackUploadedFile_whenPersistFails() {
                Long organizationId = 1L;
                Long taskId = 200L;
                Long userId = 10L;
                Long uploadedFileId = 777L;
                MockMultipartFile multipartFile = new MockMultipartFile(
                                "file", "attach.pdf", "application/pdf", "pdf-content".getBytes());

                Task task = Task.builder()
                                .id(taskId)
                                .organizationId(organizationId)
                                .title("Task")
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();

                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));
                when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());
                when(fileStorageService.uploadForCurrentUser(any(), any()))
                                .thenReturn(StoredFile.builder().id(uploadedFileId).build());
                when(taskFileRepository.existsByTaskIdAndFileId(taskId, uploadedFileId)).thenReturn(false);
                doThrow(new RuntimeException("persist error")).when(taskFileRepository).save(any());

                assertThrows(
                                RuntimeException.class,
                                () -> taskService.attachFileToTaskForCurrentUser(organizationId, taskId, multipartFile));

                verify(fileRollbackHelper).deleteFilesSafely(List.of(uploadedFileId));
        }

        @Test
        void deleteTaskFileForCurrentUser_success_removesAttachment() {
                Long organizationId = 1L;
                Long taskId = 200L;
                Long userId = 10L;
                Long fileId = 777L;

                Task task = Task.builder()
                                .id(taskId)
                                .organizationId(organizationId)
                                .title("Task")
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .build();
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();

                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));
                when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());
                when(taskFileRepository.existsByTaskIdAndFileId(taskId, fileId)).thenReturn(true);
                when(fileReferenceService.isFileReferenced(fileId)).thenReturn(false);
                when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
                when(taskMapper.toDto(any(Task.class))).thenReturn(TaskDTO.builder().id(taskId).build());
                when(taskFileRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(taskCommentRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());

                TaskDTO result = taskService.deleteTaskFileForCurrentUser(organizationId, taskId, fileId);

                assertEquals(taskId, result.getId());
                verify(taskFileRepository).deleteByTaskIdAndFileId(taskId, fileId);
                verify(fileStorageService).delete(fileId);
        }

        @Test
        void deleteTaskFileForCurrentUser_whenFileStillReferenced_doesNotDeletePhysicalFile() {
                Long organizationId = 1L;
                Long taskId = 200L;
                Long userId = 10L;
                Long fileId = 777L;

                Task task = Task.builder()
                                .id(taskId)
                                .organizationId(organizationId)
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .build();
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();

                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));
                when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());
                when(taskFileRepository.existsByTaskIdAndFileId(taskId, fileId)).thenReturn(true);
                when(fileReferenceService.isFileReferenced(fileId)).thenReturn(true);
                when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
                when(taskMapper.toDto(any(Task.class))).thenReturn(TaskDTO.builder().id(taskId).build());
                when(taskFileRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(taskCommentRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());

                taskService.deleteTaskFileForCurrentUser(organizationId, taskId, fileId);

                verify(taskFileRepository).deleteByTaskIdAndFileId(taskId, fileId);
                verify(fileStorageService, never()).delete(fileId);
        }

        @Test
        void deleteTaskFileForCurrentUser_fileNotAttached_throwsEntityNotFound() {
                Long organizationId = 1L;
                Long taskId = 200L;
                Long userId = 10L;
                Long fileId = 777L;

                Task task = Task.builder()
                                .id(taskId)
                                .organizationId(organizationId)
                                .visibility(TaskVisibility.ALL_MEMBERS)
                                .status(TaskStatus.OPEN)
                                .build();
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .build();

                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));
                when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
                when(taskVisibilityRoleRepository.findByTaskId(taskId)).thenReturn(List.of());
                when(userRoleRepository.findRolesByUserId(userId)).thenReturn(List.of());
                when(taskFileRepository.existsByTaskIdAndFileId(taskId, fileId)).thenReturn(false);

                assertThrows(
                                EntityNotFoundException.class,
                                () -> taskService.deleteTaskFileForCurrentUser(organizationId, taskId, fileId));

                verify(taskFileRepository, never()).deleteByTaskIdAndFileId(anyLong(), anyLong());
        }
}
