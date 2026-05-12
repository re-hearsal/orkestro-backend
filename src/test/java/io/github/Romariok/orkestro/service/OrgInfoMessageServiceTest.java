package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.Romariok.orkestro.messaging.dto.OrgInfoMessageDTO;
import io.github.Romariok.orkestro.messaging.models.OrgInfoMessage;
import io.github.Romariok.orkestro.messaging.repository.OrgInfoMessageRepository;
import io.github.Romariok.orkestro.messaging.service.OrgInfoMessageService;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.NotificationChannelType;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import org.springframework.context.MessageSource;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.models.enums.UserLanguageType;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrgInfoMessageServiceTest {

    @Mock
    private OrgInfoMessageRepository orgInfoMessageRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private SectionUserRepository sectionUserRepository;

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private OrgInfoMessageService orgInfoMessageService;

    private static final Long ORG_ID = 1L;
    private static final Long SECTION_ID = 10L;
    private static final Long AUTHOR_ID = 100L;
    private static final Long MEMBER_ID = 200L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orgInfoMessageService, "telegramBotMessageQueueName", "telegram_bot_messages");
        ReflectionTestUtils.setField(orgInfoMessageService, "vkBotMessageQueueName", "vk_notification_queue");
        ReflectionTestUtils.setField(orgInfoMessageService, "emailQueueName", "email_notifications");
    }

    private User buildUser(Long id, String name, NotificationChannelType channel) {
        return User.builder()
                .id(id)
                .name(name)
                .username("user" + id)
                .email("user" + id + "@test.com")
                .password("pass")
                .notificationChannel(channel)
                .preferredLanguage(UserLanguageType.RU)
                .build();
    }

    private OrganizationUser buildOrgUser(Long orgId, Long userId) {
        OrganizationUser ou = new OrganizationUser();
        ou.setOrganizationId(orgId);
        ou.setUserId(userId);
        ou.setStatus(OrganizationUserStatusType.ACCEPTED);
        return ou;
    }

    private SectionUser buildSectionUser(Long sectionId, Long userId) {
        SectionUser su = new SectionUser();
        su.setSectionId(sectionId);
        su.setUserId(userId);
        return su;
    }

    private OrgInfoMessage buildSavedMessage(Long id, Long orgId, Long sectionId, Long authorId, String text) {
        return OrgInfoMessage.builder()
                .id(id)
                .organizationId(orgId)
                .sectionId(sectionId)
                .authorUserId(authorId)
                .text(text)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void postOrgMessage_savesMessageAndNotifiesMembers() throws Exception {
        String text = "Hello org!";
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);

        OrgInfoMessage saved = buildSavedMessage(1L, ORG_ID, null, AUTHOR_ID, text);
        when(orgInfoMessageRepository.save(any())).thenReturn(saved);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        OrganizationUser member1 = buildOrgUser(ORG_ID, MEMBER_ID);
        OrganizationUser authorMember = buildOrgUser(ORG_ID, AUTHOR_ID);
        when(organizationUserRepository.findByOrganizationIdAndStatus(ORG_ID, OrganizationUserStatusType.ACCEPTED))
                .thenReturn(List.of(member1, authorMember));

        User memberUser = buildUser(MEMBER_ID, "Member", NotificationChannelType.EMAIL);
        when(userRepository.findAllById(List.of(MEMBER_ID))).thenReturn(List.of(memberUser));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OrgInfoMessageDTO result = orgInfoMessageService.postOrgMessage(ORG_ID, text);

        assertNotNull(result);
        assertEquals(ORG_ID, result.getOrganizationId());
        assertNull(result.getSectionId());
        assertEquals(text, result.getText());
        assertEquals("Author", result.getAuthorName());

        ArgumentCaptor<OrgInfoMessage> captor = ArgumentCaptor.forClass(OrgInfoMessage.class);
        verify(orgInfoMessageRepository).save(captor.capture());
        assertEquals(ORG_ID, captor.getValue().getOrganizationId());
        assertNull(captor.getValue().getSectionId());

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString());
    }

    @Test
    void postOrgMessage_doesNotNotifyAuthor() throws Exception {
        String text = "Message from author";
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);

        OrgInfoMessage saved = buildSavedMessage(2L, ORG_ID, null, AUTHOR_ID, text);
        when(orgInfoMessageRepository.save(any())).thenReturn(saved);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        // Only the author is a member
        OrganizationUser authorMember = buildOrgUser(ORG_ID, AUTHOR_ID);
        when(organizationUserRepository.findByOrganizationIdAndStatus(ORG_ID, OrganizationUserStatusType.ACCEPTED))
                .thenReturn(List.of(authorMember));

        OrgInfoMessageDTO result = orgInfoMessageService.postOrgMessage(ORG_ID, text);

        assertNotNull(result);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void postSectionMessage_savesMessageWithSectionId() throws Exception {
        String text = "Hello section!";
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);

        Section section = new Section();
        section.setId(SECTION_ID);
        section.setOrganizationId(ORG_ID);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.of(section));

        OrgInfoMessage saved = buildSavedMessage(3L, ORG_ID, SECTION_ID, AUTHOR_ID, text);
        when(orgInfoMessageRepository.save(any())).thenReturn(saved);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        SectionUser member = buildSectionUser(SECTION_ID, MEMBER_ID);
        when(sectionUserRepository.findBySectionIdOrderByJoinedAtAsc(SECTION_ID)).thenReturn(List.of(member));

        User memberUser = buildUser(MEMBER_ID, "Member", NotificationChannelType.EMAIL);
        when(userRepository.findAllById(List.of(MEMBER_ID))).thenReturn(List.of(memberUser));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OrgInfoMessageDTO result = orgInfoMessageService.postSectionMessage(SECTION_ID, text);

        assertNotNull(result);
        assertEquals(ORG_ID, result.getOrganizationId());
        assertEquals(SECTION_ID, result.getSectionId());
        assertEquals(text, result.getText());

        ArgumentCaptor<OrgInfoMessage> captor = ArgumentCaptor.forClass(OrgInfoMessage.class);
        verify(orgInfoMessageRepository).save(captor.capture());
        assertEquals(SECTION_ID, captor.getValue().getSectionId());
        assertEquals(ORG_ID, captor.getValue().getOrganizationId());

        verify(rabbitTemplate, atLeastOnce()).convertAndSend(anyString(), anyString());
    }

    @Test
    void postSectionMessage_sectionNotFound_throwsEntityNotFoundException() {
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);
        when(sectionRepository.findById(SECTION_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> orgInfoMessageService.postSectionMessage(SECTION_ID, "text"));
        verify(orgInfoMessageRepository, never()).save(any());
    }

    @Test
    void getOrgMessages_returnsPaginatedMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        OrgInfoMessage msg = buildSavedMessage(1L, ORG_ID, null, AUTHOR_ID, "msg");
        Page<OrgInfoMessage> page = new PageImpl<>(List.of(msg));

        when(orgInfoMessageRepository.findByOrganizationIdAndSectionIdIsNullOrderByCreatedAtDesc(ORG_ID, pageable))
                .thenReturn(page);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findAllById(List.of(AUTHOR_ID))).thenReturn(List.of(author));

        Page<OrgInfoMessageDTO> result = orgInfoMessageService.getOrgMessages(ORG_ID, pageable);

        assertEquals(1, result.getTotalElements());
        OrgInfoMessageDTO dto = result.getContent().get(0);
        assertEquals(ORG_ID, dto.getOrganizationId());
        assertNull(dto.getSectionId());
        assertEquals("Author", dto.getAuthorName());
        assertEquals("msg", dto.getText());
    }

    @Test
    void getSectionMessages_returnsPaginatedMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        OrgInfoMessage msg = buildSavedMessage(2L, ORG_ID, SECTION_ID, AUTHOR_ID, "section msg");
        Page<OrgInfoMessage> page = new PageImpl<>(List.of(msg));

        when(orgInfoMessageRepository.findBySectionIdOrderByCreatedAtDesc(SECTION_ID, pageable))
                .thenReturn(page);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findAllById(List.of(AUTHOR_ID))).thenReturn(List.of(author));

        Page<OrgInfoMessageDTO> result = orgInfoMessageService.getSectionMessages(SECTION_ID, pageable);

        assertEquals(1, result.getTotalElements());
        OrgInfoMessageDTO dto = result.getContent().get(0);
        assertEquals(SECTION_ID, dto.getSectionId());
        assertEquals("section msg", dto.getText());
    }

    @Test
    void postOrgMessage_authorUserNotFoundInRepository_savesMessageWithNullAuthorName() {
        String text = "Hello from ghost!";
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);

        OrgInfoMessage saved = buildSavedMessage(10L, ORG_ID, null, AUTHOR_ID, text);
        when(orgInfoMessageRepository.save(any())).thenReturn(saved);

        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.empty());

        when(organizationUserRepository.findByOrganizationIdAndStatus(ORG_ID, OrganizationUserStatusType.ACCEPTED))
                .thenReturn(List.of());

        OrgInfoMessageDTO result = orgInfoMessageService.postOrgMessage(ORG_ID, text);

        assertNotNull(result);
        assertEquals(ORG_ID, result.getOrganizationId());
        assertEquals(text, result.getText());
        assertNull(result.getAuthorName());
        verify(orgInfoMessageRepository).save(any());
    }

    @Test
    void postOrgMessage_notificationFailure_doesNotPropagateException() throws Exception {
        String text = "Notif test";
        when(securityUtils.getCurrentUserId()).thenReturn(AUTHOR_ID);

        OrgInfoMessage saved = buildSavedMessage(5L, ORG_ID, null, AUTHOR_ID, text);
        when(orgInfoMessageRepository.save(any())).thenReturn(saved);

        User author = buildUser(AUTHOR_ID, "Author", NotificationChannelType.EMAIL);
        when(userRepository.findById(AUTHOR_ID)).thenReturn(Optional.of(author));

        OrganizationUser member = buildOrgUser(ORG_ID, MEMBER_ID);
        when(organizationUserRepository.findByOrganizationIdAndStatus(ORG_ID, OrganizationUserStatusType.ACCEPTED))
                .thenReturn(List.of(member));

        User memberUser = buildUser(MEMBER_ID, "Member", NotificationChannelType.EMAIL);
        when(userRepository.findAllById(List.of(MEMBER_ID))).thenReturn(List.of(memberUser));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        // Simulate RabbitMQ failure
        org.mockito.Mockito.doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), (Object) anyString());

        // Should not throw despite notification failure
        OrgInfoMessageDTO result = orgInfoMessageService.postOrgMessage(ORG_ID, text);

        assertNotNull(result);
        assertEquals(text, result.getText());
    }
}
