package io.github.Romariok.orkestro.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.task.service.TaskNotificationService;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.MessageSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TaskNotificationServiceTest {

    @Mock
    private WebSocketNotificationService wsNotificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MessageSource messageSource;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private TaskNotificationService taskNotificationService;

    private static final Long ORG_ID = 1L;
    private static final Long TASK_ID = 100L;
    private static final String TASK_TITLE = "Test Task";
    private static final Long AUTHOR_ID = 10L;
    private static final Long ASSIGNEE_1_ID = 20L;
    private static final Long ASSIGNEE_2_ID = 30L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taskNotificationService, "emailQueueName", "email_notifications");

        // lenient: not every test triggers the org-lookup or message resolution
        org.mockito.Mockito.lenient()
                .when(organizationRepository.findById(ORG_ID))
                .thenReturn(Optional.of(Organization.builder().id(ORG_ID).name("TestOrg").build()));
        org.mockito.Mockito.lenient()
                .when(messageSource.getMessage(anyString(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- helpers ----

    private User emailUser(Long id) {
        return User.builder()
                .id(id)
                .email("user" + id + "@test.com")
                .notificationChannel(NotificationChannelType.EMAIL)
                .preferredLanguage(UserLanguageType.EN)
                .build();
    }

    private void stubUsers(User... users) {
        for (User u : users) {
            // lenient: some users may be excluded from recipients and never looked up
            org.mockito.Mockito.lenient()
                    .when(userRepository.findById(u.getId()))
                    .thenReturn(Optional.of(u));
        }
    }

    private List<Long> captureWsSentUserIds(int expectedTimes) {
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(expectedTimes)).send(captor.capture(), any(InAppNotificationDTO.class));
        return captor.getAllValues();
    }

    // ======================================================
    // notifyTaskUpdated
    // ======================================================

    @Test
    void notifyTaskUpdated_sendsWsToAuthorAndAssignees_excludesInitiator() {
        User author = emailUser(AUTHOR_ID);
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        User assignee2 = emailUser(ASSIGNEE_2_ID);
        stubUsers(author, assignee1, assignee2);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(ASSIGNEE_1_ID, ASSIGNEE_2_ID),
                ASSIGNEE_1_ID);  // ASSIGNEE_1 is the initiator

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(2)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactlyInAnyOrder(AUTHOR_ID, ASSIGNEE_2_ID)
                .doesNotContain(ASSIGNEE_1_ID);
    }

    @Test
    void notifyTaskUpdated_whenInitiatorIsAuthor_sendsOnlyToAssignees() {
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        User assignee2 = emailUser(ASSIGNEE_2_ID);
        stubUsers(assignee1, assignee2);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(ASSIGNEE_1_ID, ASSIGNEE_2_ID),
                AUTHOR_ID);  // author is the initiator

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(2)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactlyInAnyOrder(ASSIGNEE_1_ID, ASSIGNEE_2_ID)
                .doesNotContain(AUTHOR_ID);
    }

    @Test
    void notifyTaskUpdated_withNoAssignees_sendsOnlyToAuthor() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(),
                999L);  // unknown initiator

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(1)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues()).containsExactly(AUTHOR_ID);
    }

    @Test
    void notifyTaskUpdated_broadcastsToOrgTopic() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/organizations/" + ORG_ID + "/tasks"),
                any(Object.class));
    }

    @Test
    void notifyTaskUpdated_wsNotificationCarriesCorrectTypeAndEntityId() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        ArgumentCaptor<InAppNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(AUTHOR_ID), dtoCaptor.capture());

        InAppNotificationDTO dto = dtoCaptor.getValue();
        assertThat(dto.getType()).isEqualTo(InAppNotificationType.TASK_UPDATED);
        assertThat(dto.getEntityId()).isEqualTo(TASK_ID);
        assertThat(dto.getEntityType()).isEqualTo("TASK");
    }

    // ======================================================
    // notifyTaskDeleted
    // ======================================================

    @Test
    void notifyTaskDeleted_sendsWsToAuthorAndAssignees_excludesInitiator() {
        User author = emailUser(AUTHOR_ID);
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        stubUsers(author, assignee1);

        taskNotificationService.notifyTaskDeleted(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(ASSIGNEE_1_ID),
                AUTHOR_ID);  // author is initiator

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(1)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactly(ASSIGNEE_1_ID)
                .doesNotContain(AUTHOR_ID);
    }

    @Test
    void notifyTaskDeleted_wsNotificationCarriesDeletedType() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskDeleted(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        ArgumentCaptor<InAppNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(AUTHOR_ID), dtoCaptor.capture());

        assertThat(dtoCaptor.getValue().getType()).isEqualTo(InAppNotificationType.TASK_DELETED);
    }

    @Test
    void notifyTaskDeleted_broadcastsToOrgTopic() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskDeleted(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/organizations/" + ORG_ID + "/tasks"),
                any(Object.class));
    }

    // ======================================================
    // notifyAssigneeAdded
    // ======================================================

    @Test
    void notifyAssigneeAdded_sendsWsOnlyToAddedUser() {
        User added = emailUser(ASSIGNEE_1_ID);
        when(userRepository.findById(ASSIGNEE_1_ID)).thenReturn(Optional.of(added));

        taskNotificationService.notifyAssigneeAdded(ORG_ID, TASK_ID, TASK_TITLE, ASSIGNEE_1_ID);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(1)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getValue()).isEqualTo(ASSIGNEE_1_ID);
    }

    @Test
    void notifyAssigneeAdded_wsNotificationCarriesAssigneeAddedType() {
        User added = emailUser(ASSIGNEE_1_ID);
        when(userRepository.findById(ASSIGNEE_1_ID)).thenReturn(Optional.of(added));

        taskNotificationService.notifyAssigneeAdded(ORG_ID, TASK_ID, TASK_TITLE, ASSIGNEE_1_ID);

        ArgumentCaptor<InAppNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(ASSIGNEE_1_ID), dtoCaptor.capture());

        assertThat(dtoCaptor.getValue().getType()).isEqualTo(InAppNotificationType.TASK_ASSIGNEE_ADDED);
        assertThat(dtoCaptor.getValue().getEntityId()).isEqualTo(TASK_ID);
    }

    @Test
    void notifyAssigneeAdded_userNotFound_doesNotSendWs() {
        when(userRepository.findById(ASSIGNEE_1_ID)).thenReturn(Optional.empty());

        taskNotificationService.notifyAssigneeAdded(ORG_ID, TASK_ID, TASK_TITLE, ASSIGNEE_1_ID);

        verify(wsNotificationService, never()).send(anyLong(), any());
    }

    // ======================================================
    // notifyAssigneeRemoved
    // ======================================================

    @Test
    void notifyAssigneeRemoved_sendsWsOnlyToRemovedUser() {
        User removed = emailUser(ASSIGNEE_2_ID);
        when(userRepository.findById(ASSIGNEE_2_ID)).thenReturn(Optional.of(removed));

        taskNotificationService.notifyAssigneeRemoved(ORG_ID, TASK_ID, TASK_TITLE, ASSIGNEE_2_ID);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(1)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getValue()).isEqualTo(ASSIGNEE_2_ID);
    }

    @Test
    void notifyAssigneeRemoved_wsNotificationCarriesAssigneeRemovedType() {
        User removed = emailUser(ASSIGNEE_2_ID);
        when(userRepository.findById(ASSIGNEE_2_ID)).thenReturn(Optional.of(removed));

        taskNotificationService.notifyAssigneeRemoved(ORG_ID, TASK_ID, TASK_TITLE, ASSIGNEE_2_ID);

        ArgumentCaptor<InAppNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(ASSIGNEE_2_ID), dtoCaptor.capture());

        assertThat(dtoCaptor.getValue().getType()).isEqualTo(InAppNotificationType.TASK_ASSIGNEE_REMOVED);
    }

    // ======================================================
    // notifyAssigneesChanged
    // ======================================================

    @Test
    void notifyAssigneesChanged_sendsWsToParticipants_excludesInitiatorAndNewlyAdded() {
        User author = emailUser(AUTHOR_ID);
        User assignee2 = emailUser(ASSIGNEE_2_ID);
        stubUsers(author, assignee2);

        taskNotificationService.notifyAssigneesChanged(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID,
                List.of(ASSIGNEE_1_ID, ASSIGNEE_2_ID),
                AUTHOR_ID,               // initiator = author → excluded
                List.of(ASSIGNEE_1_ID)); // ASSIGNEE_1 was newly added → excluded

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(1)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactly(ASSIGNEE_2_ID)
                .doesNotContain(AUTHOR_ID, ASSIGNEE_1_ID);
    }

    @Test
    void notifyAssigneesChanged_withNoExclusions_sendsToAllParticipants() {
        User author = emailUser(AUTHOR_ID);
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        User assignee2 = emailUser(ASSIGNEE_2_ID);
        stubUsers(author, assignee1, assignee2);

        // Use overload without newlyAddedUserIds, initiator is someone outside
        taskNotificationService.notifyAssigneesChanged(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID,
                List.of(ASSIGNEE_1_ID, ASSIGNEE_2_ID),
                999L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(3)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactlyInAnyOrder(AUTHOR_ID, ASSIGNEE_1_ID, ASSIGNEE_2_ID);
    }

    @Test
    void notifyAssigneesChanged_broadcastsToOrgTopic() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyAssigneesChanged(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/organizations/" + ORG_ID + "/tasks"),
                any(Object.class));
    }

    // ======================================================
    // notifyDeadlineOverdue
    // ======================================================

    @Test
    void notifyDeadlineOverdue_sendsWsToAllParticipants_noInitiatorExclusion() {
        User author = emailUser(AUTHOR_ID);
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        stubUsers(author, assignee1);

        taskNotificationService.notifyDeadlineOverdue(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(ASSIGNEE_1_ID));

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(2)).send(captor.capture(), any(InAppNotificationDTO.class));

        assertThat(captor.getAllValues())
                .containsExactlyInAnyOrder(AUTHOR_ID, ASSIGNEE_1_ID);
    }

    @Test
    void notifyDeadlineOverdue_wsNotificationCarriesDeadlineOverdueType() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyDeadlineOverdue(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of());

        ArgumentCaptor<InAppNotificationDTO> dtoCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(AUTHOR_ID), dtoCaptor.capture());

        assertThat(dtoCaptor.getValue().getType()).isEqualTo(InAppNotificationType.TASK_DEADLINE_OVERDUE);
    }

    // ======================================================
    // Deduplication — author also listed in assignees
    // ======================================================

    @Test
    void notifyTaskUpdated_deduplicatesRecipients_whenAuthorAlsoInAssignees() {
        User author = emailUser(AUTHOR_ID);
        User assignee1 = emailUser(ASSIGNEE_1_ID);
        stubUsers(author, assignee1);

        // AUTHOR_ID appears in both author and assignees list
        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(AUTHOR_ID, ASSIGNEE_1_ID),
                999L);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(wsNotificationService, times(2)).send(captor.capture(), any(InAppNotificationDTO.class));

        // AUTHOR_ID should appear only once
        assertThat(captor.getAllValues()).containsExactlyInAnyOrder(AUTHOR_ID, ASSIGNEE_1_ID);
    }

    // ======================================================
    // Org topic payload content
    // ======================================================

    @Test
    void notifyTaskUpdated_orgTopicPayloadContainsTaskIdAndType() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskUpdated(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> payloadCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/organizations/" + ORG_ID + "/tasks"),
                (Object) payloadCaptor.capture());

        java.util.Map<String, Object> payload = payloadCaptor.getValue();
        assertThat(payload.get("taskId")).isEqualTo(TASK_ID);
        assertThat(payload.get("type")).isEqualTo("TASK_UPDATED");
    }

    @Test
    void notifyTaskDeleted_orgTopicPayloadContainsDeletedType() {
        User author = emailUser(AUTHOR_ID);
        stubUsers(author);

        taskNotificationService.notifyTaskDeleted(
                ORG_ID, TASK_ID, TASK_TITLE,
                AUTHOR_ID, List.of(), 999L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> payloadCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/organizations/" + ORG_ID + "/tasks"),
                (Object) payloadCaptor.capture());

        assertThat(payloadCaptor.getValue().get("type")).isEqualTo("TASK_DELETED");
    }
}
