package io.github.Romariok.orkestro.event.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.event.dto.EventFeedbackRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventFeedbackRowDTO;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventComment;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventCommentRepository;
import io.github.Romariok.orkestro.event.repository.EventDescriptionTemplateRepository;
import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.repository.EventSectionRepository;
import io.github.Romariok.orkestro.event.repository.EventSongRepository;
import io.github.Romariok.orkestro.event.mapper.EventMapper;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.repertoire.repository.SongRepository;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.security.OrganizationPermissionChecker;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class EventCommentFeedbackServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private EventCommentRepository eventCommentRepository;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private EventFileRepository eventFileRepository;
    @Mock private EventSongRepository eventSongRepository;
    @Mock private EventSectionRepository eventSectionRepository;
    @Mock private SongRepository songRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationUserRepository organizationUserRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private SectionUserRepository sectionUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventMapper eventMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private OrganizationPermissionChecker organizationPermissionChecker;
    @Mock private FileStorageService fileStorageService;
    @Mock private FileRollbackHelper fileRollbackHelper;
    @Mock private FileReferenceService fileReferenceService;
    @Mock private FileLimitsProperties fileLimitsProperties;
    @Mock private EventDescriptionTemplateRepository eventDescriptionTemplateRepository;
    @Mock private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private EventService eventService;

    private static final Long ORG_ID = 1L;
    private static final Long USER_ID = 42L;

    @BeforeEach
    void setup() {
        lenient().when(fileLimitsProperties.getEventMaxFiles()).thenReturn(50);
        lenient().when(eventSectionRepository.findByEventId(any())).thenReturn(List.of());
        lenient().when(eventSectionRepository.findByEventIdIn(anyCollection())).thenReturn(List.of());

        when(securityUtils.getCurrentUserId()).thenReturn(USER_ID);

        OrganizationUser membership = OrganizationUser.builder()
                .organizationId(ORG_ID)
                .userId(USER_ID)
                .status(OrganizationUserStatusType.ACCEPTED)
                .joinedAt(Instant.now())
                .build();
        when(organizationUserRepository.findByOrganizationIdAndUserId(ORG_ID, USER_ID))
                .thenReturn(Optional.of(membership));
    }

    @Test
    void getEventFeedbackForCurrentUser_returnsPageOfRows() {
        EventComment comment = EventComment.builder()
                .id(10L)
                .eventId(20L)
                .authorUserId(USER_ID)
                .text("Great rehearsal")
                .rating(5)
                .createdAt(Instant.parse("2026-03-01T10:00:00Z"))
                .build();

        Event event = Event.builder()
                .id(20L)
                .organizationId(ORG_ID)
                .title("Spring Concert")
                .eventType(EventType.CONCERT)
                .startTime(Instant.parse("2026-03-01T08:00:00Z"))
                .endTime(Instant.parse("2026-03-01T10:00:00Z"))
                .tags(Set.of("strings"))
                .build();

        User author = User.builder()
                .id(USER_ID)
                .name("Alice")
                .profileImageFileId(5L)
                .build();

        when(eventCommentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(comment)));
        when(userRepository.findAllById(Set.of(USER_ID))).thenReturn(List.of(author));
        when(eventRepository.findAllById(Set.of(20L))).thenReturn(List.of(event));

        Page<EventFeedbackRowDTO> result = eventService.getEventFeedbackForCurrentUser(
                ORG_ID, new EventFeedbackRequestDTO(), PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        EventFeedbackRowDTO row = result.getContent().get(0);
        assertEquals(10L, row.getCommentId());
        assertEquals("Great rehearsal", row.getCommentText());
        assertEquals(5, row.getRating());
        assertEquals("Alice", row.getAuthorName());
        assertEquals("Spring Concert", row.getEventTitle());
        assertEquals(EventType.CONCERT, row.getEventType());
    }

    @Test
    void getEventFeedbackForCurrentUser_withEventTypeFilter_appliesSpec() {
        EventFeedbackRequestDTO request = EventFeedbackRequestDTO.builder()
                .eventType(EventType.REHEARSAL)
                .build();

        when(eventCommentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findAllById(Set.of())).thenReturn(List.of());
        when(eventRepository.findAllById(Set.of())).thenReturn(List.of());

        Page<EventFeedbackRowDTO> result = eventService.getEventFeedbackForCurrentUser(
                ORG_ID, request, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getEventFeedbackForCurrentUser_withEventStartTimeSort_doesNotThrow() {
        EventFeedbackRequestDTO request = EventFeedbackRequestDTO.builder()
                .sortField("eventStartTime")
                .build();

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "eventStartTime"));

        when(eventCommentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(userRepository.findAllById(Set.of())).thenReturn(List.of());
        when(eventRepository.findAllById(Set.of())).thenReturn(List.of());

        Page<EventFeedbackRowDTO> result = eventService.getEventFeedbackForCurrentUser(
                ORG_ID, request, pageable);

        assertNotNull(result);
    }
}
