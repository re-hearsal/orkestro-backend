package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.service.OrgNotificationService;
import io.github.Romariok.orkestro.user.models.Permission;
import io.github.Romariok.orkestro.user.models.Role;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.UserRole;
import io.github.Romariok.orkestro.user.models.enums.RoleScopeType;
import io.github.Romariok.orkestro.user.repository.RolePermissionRepository;
import io.github.Romariok.orkestro.user.repository.RoleRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.user.repository.UserRoleRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import java.util.List;
import java.util.Locale;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrgNotificationServiceTest {

    @Mock
    private WebSocketNotificationService wsNotificationService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private MessageSource messageSource;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrgNotificationService orgNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orgNotificationService, "emailQueueName", "email_notifications");
    }

    @Test
    void notifyJoinRequestReceived_emailChannel_sendsWsAndEmailToLeader() throws Exception {
        Long organizationId = 1L;
        Long requesterUserId = 10L;
        Long leaderUserId = 20L;
        Long roleId = 100L;

        Role leaderRole = Role.builder()
                .id(roleId)
                .scope(RoleScopeType.ORGANIZATION)
                .organizationId(organizationId)
                .name("Leader")
                .build();

        Permission viewPermission = Permission.builder()
                .code("ORG_JOIN_REQUEST_VIEW")
                .description("Can view join requests")
                .build();

        OrganizationUser acceptedRequester = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(requesterUserId)
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();

        OrganizationUser acceptedLeader = OrganizationUser.builder()
                .organizationId(organizationId)
                .userId(leaderUserId)
                .status(OrganizationUserStatusType.ACCEPTED)
                .build();

        User requester = User.builder()
                .id(requesterUserId)
                .username("new_user")
                .build();

        User leader = User.builder()
                .id(leaderUserId)
                .email("leader@example.com")
                .notificationChannel(NotificationChannelType.EMAIL)
                .build();

        when(roleRepository.findByScopeAndOrganizationId(RoleScopeType.ORGANIZATION, organizationId))
                .thenReturn(List.of(leaderRole));
        when(rolePermissionRepository.findPermissionsByRoleId(roleId)).thenReturn(List.of(viewPermission));
        when(userRoleRepository.findByRoleId(roleId)).thenReturn(List.of(
                UserRole.builder().userId(requesterUserId).roleId(roleId).build(),
                UserRole.builder().userId(leaderUserId).roleId(roleId).build()));
        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, requesterUserId))
                .thenReturn(Optional.of(acceptedRequester));
        when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, leaderUserId))
                .thenReturn(Optional.of(acceptedLeader));
        when(userRepository.findAllById(List.of(requesterUserId))).thenReturn(List.of(requester));
        when(userRepository.findById(leaderUserId)).thenReturn(Optional.of(leader));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("notification text");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"payload\":true}");

        orgNotificationService.notifyJoinRequestReceived(organizationId, requesterUserId, "Orkestro");

        ArgumentCaptor<InAppNotificationDTO> wsCaptor = ArgumentCaptor.forClass(InAppNotificationDTO.class);
        verify(wsNotificationService).send(eq(leaderUserId), wsCaptor.capture());
        assertEquals(InAppNotificationType.JOIN_REQUEST_RECEIVED, wsCaptor.getValue().getType());

        verify(rabbitTemplate).convertAndSend(eq("email_notifications"), eq("{\"payload\":true}"));
        verify(userRepository, never()).findById(requesterUserId);
    }
}
