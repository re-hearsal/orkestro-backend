package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarGroupedResponseDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarScope;
import io.github.Romariok.orkestro.event.dto.EventCommentCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventPageDTO;
import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventUserCalendarRequestDTO;
import io.github.Romariok.orkestro.event.mapper.EventMapper;
import io.github.Romariok.orkestro.event.models.EventComment;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventFile;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventSection;
import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventParticipantSourceType;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventCommentRepository;
import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.event.repository.EventSongRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.repository.EventSectionRepository;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.security.OrganizationPermissionChecker;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

        @Mock
        private EventRepository eventRepository;

        @Mock
        private EventCommentRepository eventCommentRepository;

        @Mock
        private EventParticipantRepository eventParticipantRepository;

        @Mock
        private OrganizationRepository organizationRepository;

        @Mock
        private OrganizationUserRepository organizationUserRepository;

        @Mock
        private SectionRepository sectionRepository;

        @Mock
        private SectionUserRepository sectionUserRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private EventMapper eventMapper;

        @Mock
        private SecurityUtils securityUtils;

        @Mock
        private OrganizationPermissionChecker organizationPermissionChecker;

        @Mock
        private EventFileRepository eventFileRepository;

        @Mock
        private EventSongRepository eventSongRepository;

        @Mock
        private EventSectionRepository eventSectionRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private FileRollbackHelper fileRollbackHelper;

        @Mock
        private FileReferenceService fileReferenceService;

        @Mock
        private FileLimitsProperties fileLimitsProperties;

        @InjectMocks
        private EventService eventService;

        @Mock
        private EventNotificationService eventNotificationService;

        @BeforeEach
        void setup() {
                lenient().when(fileLimitsProperties.getEventMaxFiles()).thenReturn(50);
                lenient().when(eventSectionRepository.findByEventId(anyLong())).thenReturn(List.of());
                lenient().when(eventSectionRepository.findByEventIdIn(anyCollection())).thenReturn(List.of());
        }

        @Test
        void createEventInOrganization_success_savesEventAndParticipants() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long userId1 = 10L;
                Long userId2 = 20L;

                Instant start = Instant.parse("2025-01-01T10:00:00Z");
                Instant end = Instant.parse("2025-01-01T12:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Rehearsal")
                                .description("Section rehearsal")
                                .eventType(EventType.REHEARSAL)
                                .location("Hall 1")
                                .startTime(start)
                                .endTime(end)
                                .participantUserIds(List.of(userId1, userId2))
                                .tags(List.of("strings", "morning"))
                                .build();

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser currentMembership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(currentMembership));

                User user1 = User.builder().id(userId1).username("u1").email("u1@example.com").password("p")
                                .profileImageFileId(1L).build();
                User user2 = User.builder().id(userId2).username("u2").email("u2@example.com").password("p")
                                .profileImageFileId(1L).build();
                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1, user2));

                OrganizationUser ou1 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId1)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                OrganizationUser ou2 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId2)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndStatus(organizationId,
                                OrganizationUserStatusType.ACCEPTED))
                                .thenReturn(List.of(ou1, ou2));

                Event saved = Event.builder()
                                .id(100L)
                                .organizationId(organizationId)
                                .title("Rehearsal")
                                .description("Section rehearsal")
                                .eventType(EventType.REHEARSAL)
                                .location("Hall 1")
                                .startTime(start)
                                .endTime(end)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.save(any(Event.class))).thenReturn(saved);

                when(eventParticipantRepository.findByEventId(100L)).thenReturn(List.of());
                when(eventFileRepository.findByEventId(100L)).thenReturn(List.of());

                when(eventMapper.toDto(any(Event.class))).thenAnswer(invocation -> {
                        Event e = invocation.getArgument(0);
                        return EventDTO.builder()
                                        .id(e.getId())
                                        .organizationId(e.getOrganizationId())
                                        .title(e.getTitle())
                                        .description(e.getDescription())
                                        .eventType(e.getEventType())
                                        .location(e.getLocation())
                                        .startTime(e.getStartTime())
                                        .endTime(e.getEndTime())
                                        .createdAt(e.getCreatedAt())
                                        .build();
                });

                EventDTO result = eventService.createEventInOrganization(organizationId, request);

                ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
                verify(eventRepository).save(eventCaptor.capture());
                Event persisted = eventCaptor.getValue();

                assertEquals(organizationId, persisted.getOrganizationId());
                assertEquals("Rehearsal", persisted.getTitle());
                assertEquals("Section rehearsal", persisted.getDescription());
                assertEquals(EventType.REHEARSAL, persisted.getEventType());
                assertEquals(start, persisted.getStartTime());
                assertEquals(end, persisted.getEndTime());
                assertFalse(persisted.isIncludeAllOrganizationMembers());

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<EventParticipant>> participantsCaptor = (ArgumentCaptor<List<EventParticipant>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(eventParticipantRepository).saveAll(participantsCaptor.capture());
                List<EventParticipant> participants = participantsCaptor.getValue();
                assertEquals(2, participants.size());

                EventParticipant p1 = participants.get(0);
                assertEquals(100L, p1.getEventId());
                assertEquals(EventParticipantSourceType.MANUAL, p1.getSource());
                assertEquals(EventRsvpStatus.PENDING, p1.getRsvpStatus());
                assertEquals(EventAttendanceStatus.UNKNOWN, p1.getAttendanceStatus());

                assertEquals("Rehearsal", result.getTitle());
                assertEquals(organizationId, result.getOrganizationId());
        }

        @Test
        void createEventInOrganization_withSendRsvpTrue_sendsNotifications() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long userId1 = 10L;

                Instant start = Instant.parse("2025-01-01T10:00:00Z");
                Instant end = Instant.parse("2025-01-01T12:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Event with RSVP")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .participantUserIds(List.of(userId1))
                                .sendRsvp(true)
                                .build();

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser currentMembership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(currentMembership));

                User user1 = User.builder().id(userId1).username("u1").email("u1@example.com").password("p")
                                .profileImageFileId(1L).build();
                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1));

                OrganizationUser ou1 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId1)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndStatus(organizationId,
                                OrganizationUserStatusType.ACCEPTED))
                                .thenReturn(List.of(ou1));

                Event saved = Event.builder()
                                .id(200L)
                                .organizationId(organizationId)
                                .title("Event with RSVP")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.save(any(Event.class))).thenReturn(saved);
                when(eventParticipantRepository.findByEventId(200L)).thenReturn(List.of());
                when(eventMapper.toDto(any(Event.class))).thenReturn(
                                EventDTO.builder().id(200L).organizationId(organizationId).title("Event with RSVP")
                                                .eventType(EventType.CONCERT).startTime(start).endTime(end)
                                                .createdAt(Instant.now()).build());

                eventService.createEventInOrganization(organizationId, request);

                verify(eventNotificationService).sendEventCreatedNotifications(any(Event.class), anyCollection());
        }

        @Test
        void createEventInOrganization_withAllOrganizationMembers_includesAllAcceptedUsers() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long userId1 = 10L;
                Long userId2 = 20L;

                Instant start = Instant.parse("2025-02-01T10:00:00Z");
                Instant end = Instant.parse("2025-02-01T12:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Full orchestra rehearsal")
                                .eventType(EventType.REHEARSAL)
                                .startTime(start)
                                .endTime(end)
                                .includeAllOrganizationMembers(true)
                                .build();

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();
                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser currentMembership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(currentMembership));

                OrganizationUser ou1 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId1)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                OrganizationUser ou2 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId2)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndStatus(organizationId,
                                OrganizationUserStatusType.ACCEPTED))
                                .thenReturn(List.of(ou1, ou2));

                User user1 = User.builder().id(userId1).username("u1").email("u1@example.com").password("p")
                                .profileImageFileId(1L).build();
                User user2 = User.builder().id(userId2).username("u2").email("u2@example.com").password("p")
                                .profileImageFileId(1L).build();
                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1, user2));

                Event saved = Event.builder()
                                .id(200L)
                                .organizationId(organizationId)
                                .title("Full orchestra rehearsal")
                                .eventType(EventType.REHEARSAL)
                                .startTime(start)
                                .endTime(end)
                                .createdAt(Instant.now())
                                .build();
                when(eventRepository.save(any(Event.class))).thenReturn(saved);

                when(eventParticipantRepository.findByEventId(200L)).thenReturn(List.of());
                when(eventMapper.toDto(any(Event.class))).thenAnswer(invocation -> {
                        Event e = invocation.getArgument(0);
                        return EventDTO.builder()
                                        .id(e.getId())
                                        .organizationId(e.getOrganizationId())
                                        .title(e.getTitle())
                                        .eventType(e.getEventType())
                                        .startTime(e.getStartTime())
                                        .endTime(e.getEndTime())
                                        .createdAt(e.getCreatedAt())
                                        .build();
                });

                EventDTO result = eventService.createEventInOrganization(organizationId, request);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<EventParticipant>> participantsCaptor = (ArgumentCaptor<List<EventParticipant>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(eventParticipantRepository).saveAll(participantsCaptor.capture());
                List<EventParticipant> participants = participantsCaptor.getValue();

                participants.sort(Comparator.comparing(EventParticipant::getUserId));
                assertEquals(2, participants.size());
                assertEquals(userId1, participants.get(0).getUserId());
                assertEquals(EventParticipantSourceType.ORGANIZATION, participants.get(0).getSource());
                assertEquals(userId2, participants.get(1).getUserId());
                assertEquals(EventParticipantSourceType.ORGANIZATION, participants.get(1).getSource());
                verify(eventSectionRepository, never()).saveAll(anyCollection());

                assertEquals("Full orchestra rehearsal", result.getTitle());
        }

        @Test
        void createEventInOrganization_withSectionsAndManualParticipants_mergesAndResolvesSources() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long sectionId = 100L;
                Long userId1 = 10L;
                Long userId2 = 20L;
                Long userId3 = 30L;

                Instant start = Instant.parse("2025-03-01T10:00:00Z");
                Instant end = Instant.parse("2025-03-01T12:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Section plus soloists")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .participantSectionIds(List.of(sectionId))
                                .participantUserIds(List.of(userId2, userId3))
                                .build();

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();
                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser currentMembership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(currentMembership));

                // Section in the same organization
                Section section = new Section();
                section.setId(sectionId);
                section.setOrganizationId(organizationId);
                section.setName("Strings");
                when(sectionRepository.findAllById(anyCollection())).thenReturn(List.of(section));

                // Users in the section: user1, user2
                SectionUser su1 = new SectionUser();
                su1.setSectionId(sectionId);
                su1.setUserId(userId1);
                su1.setJoinedAt(Instant.now());

                SectionUser su2 = new SectionUser();
                su2.setSectionId(sectionId);
                su2.setUserId(userId2);
                su2.setJoinedAt(Instant.now());

                when(sectionUserRepository.findBySectionIdIn(anyCollection())).thenReturn(List.of(su1, su2));

                OrganizationUser ou1 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId1)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                OrganizationUser ou2 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId2)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                OrganizationUser ou3 = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId3)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndStatus(organizationId,
                                OrganizationUserStatusType.ACCEPTED))
                                .thenReturn(List.of(ou1, ou2, ou3));

                User user1 = User.builder().id(userId1).username("u1").email("u1@example.com").password("p")
                                .profileImageFileId(1L).build();
                User user2 = User.builder().id(userId2).username("u2").email("u2@example.com").password("p")
                                .profileImageFileId(1L).build();
                User user3 = User.builder().id(userId3).username("u3").email("u3@example.com").password("p")
                                .profileImageFileId(1L).build();
                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1, user2, user3));

                Event saved = Event.builder()
                                .id(300L)
                                .organizationId(organizationId)
                                .title("Section plus soloists")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .createdAt(Instant.now())
                                .build();
                when(eventRepository.save(any(Event.class))).thenReturn(saved);

                when(eventParticipantRepository.findByEventId(300L)).thenReturn(List.of());
                when(eventMapper.toDto(any(Event.class))).thenAnswer(invocation -> {
                        Event e = invocation.getArgument(0);
                        return EventDTO.builder()
                                        .id(e.getId())
                                        .organizationId(e.getOrganizationId())
                                        .title(e.getTitle())
                                        .eventType(e.getEventType())
                                        .startTime(e.getStartTime())
                                        .endTime(e.getEndTime())
                                        .createdAt(e.getCreatedAt())
                                        .build();
                });

                EventDTO result = eventService.createEventInOrganization(organizationId, request);

                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<EventParticipant>> participantsCaptor = (ArgumentCaptor<List<EventParticipant>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(eventParticipantRepository).saveAll(participantsCaptor.capture());
                List<EventParticipant> participants = participantsCaptor.getValue();

                assertEquals(3, participants.size());
                participants.sort(Comparator.comparing(EventParticipant::getUserId));

                EventParticipant pUser1 = participants.get(0);
                EventParticipant pUser2 = participants.get(1);
                EventParticipant pUser3 = participants.get(2);

                assertEquals(userId1, pUser1.getUserId());
                assertEquals(EventParticipantSourceType.SECTION, pUser1.getSource());

                assertEquals(userId2, pUser2.getUserId());
                // user2 пришёл из секции и из ручного списка — должен получить MANUAL
                assertEquals(EventParticipantSourceType.MANUAL, pUser2.getSource());

                assertEquals(userId3, pUser3.getUserId());
                assertEquals(EventParticipantSourceType.MANUAL, pUser3.getSource());
                @SuppressWarnings("unchecked")
                ArgumentCaptor<List<EventSection>> eventSectionsCaptor = (ArgumentCaptor<List<EventSection>>) (ArgumentCaptor<?>) ArgumentCaptor
                                .forClass(List.class);
                verify(eventSectionRepository).saveAll(eventSectionsCaptor.capture());
                List<EventSection> eventSections = eventSectionsCaptor.getValue();
                assertEquals(1, eventSections.size());
                assertEquals(sectionId, eventSections.get(0).getSectionId());

                assertEquals("Section plus soloists", result.getTitle());
        }

        @Test
        void createEventInOrganization_blankTitle_throwsIllegalArgumentException() {
                Long organizationId = 1L;

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("   ")
                                .eventType(EventType.CONCERT)
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .build();

                assertThrows(IllegalArgumentException.class,
                                () -> eventService.createEventInOrganization(organizationId, request));

                verify(organizationRepository, never()).findById(anyLong());
                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventInOrganization_invalidTimeRange_throwsIllegalArgumentException() {
                Long organizationId = 1L;

                Instant start = Instant.parse("2025-01-01T10:00:00Z");
                Instant end = Instant.parse("2025-01-01T09:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Event")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .build();

                assertThrows(IllegalArgumentException.class,
                                () -> eventService.createEventInOrganization(organizationId, request));

                verify(organizationRepository, never()).findById(anyLong());
                verify(eventRepository, never()).save(any());
        }

        @Test
        void createEventInOrganization_participantNotMember_throwsBusinessException() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long userId = 10L;

                Instant start = Instant.parse("2025-01-01T10:00:00Z");
                Instant end = Instant.parse("2025-01-01T12:00:00Z");

                EventCreateRequestDTO request = EventCreateRequestDTO.builder()
                                .title("Event")
                                .eventType(EventType.CONCERT)
                                .startTime(start)
                                .endTime(end)
                                .participantUserIds(List.of(userId))
                                .build();

                Organization organization = Organization.builder()
                                .id(organizationId)
                                .name("Orchestra")
                                .location("City")
                                .profileImageFileId(100L)
                                .build();

                when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser currentMembership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(currentMembership));

                User user = User.builder().id(userId).username("u1").email("u1@example.com").password("p")
                                .profileImageFileId(1L).build();
                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));

                when(organizationUserRepository.findByOrganizationIdAndStatus(organizationId,
                                OrganizationUserStatusType.ACCEPTED))
                                .thenReturn(List.of());

                assertThrows(
                                BusinessException.class,
                                () -> eventService.createEventInOrganization(organizationId, request));

                verify(eventRepository, never()).save(any());
                verify(eventParticipantRepository, never()).saveAll(any());
        }

        @Test
        void getEventForCurrentUser_notMember_throwsBusinessException() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long userId = 10L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.empty());

                assertThrows(BusinessException.class, () -> eventService.getEventForCurrentUser(organizationId, eventId));
        }

        @Test
        void getEventForCurrentUser_success_returnsDto() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long userId = 10L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                OrganizationUser ou = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(ou));

                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of());

                when(eventMapper.toDto(any(Event.class))).thenAnswer(invocation -> {
                        Event e = invocation.getArgument(0);
                        return EventDTO.builder()
                                        .id(e.getId())
                                        .organizationId(e.getOrganizationId())
                                        .title(e.getTitle())
                                        .startTime(e.getStartTime())
                                        .endTime(e.getEndTime())
                                        .createdAt(e.getCreatedAt())
                                        .build();
                });

                EventDTO dto = eventService.getEventForCurrentUser(organizationId, eventId);

                assertEquals(eventId, dto.getId());
                assertEquals(organizationId, dto.getOrganizationId());
                assertEquals("Event", dto.getTitle());
        }

        @Test
        void getCalendarForCurrentUserInOrganization_scopeOrganization_returnsCalendarPage() {
                Long organizationId = 1L;
                Long userId = 10L;
                Instant from = Instant.parse("2030-01-01T00:00:00Z");
                Instant to = Instant.parse("2030-01-05T00:00:00Z");
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

                Event event1 = Event.builder()
                                .id(1L)
                                .organizationId(organizationId)
                                .title("Calendar event")
                                .eventType(EventType.REHEARSAL)
                                .location("Hall 1")
                                .includeAllOrganizationMembers(true)
                                .startTime(from.plusSeconds(3600))
                                .endTime(from.plusSeconds(7200))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Event>>any(), eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(event1), pageable, 1));

                EventCalendarRequestDTO request = new EventCalendarRequestDTO();
                request.setScope(EventCalendarScope.ORGANIZATION.name());
                request.setFrom(from);
                request.setTo(to);

                when(eventSectionRepository.findByEventIdIn(anyCollection()))
                                .thenReturn(List.of());

                EventCalendarGroupedResponseDTO result = eventService.getCalendarForCurrentUserInOrganization(
                                organizationId, request, pageable);

                assertEquals(1, result.getTotalElements());
                assertEquals(0, result.getSectionGroups().size());
                assertEquals(1, result.getOrganizationWideEvents().size());
                EventCalendarDTO dto = result.getOrganizationWideEvents().get(0);
                assertEquals(1L, dto.getId());
                assertEquals("Calendar event", dto.getTitle());
                assertEquals(EventType.REHEARSAL, dto.getEventType());
                assertEquals("Hall 1", dto.getLocation());
        }

        @Test
        void getCurrentUserCalendarInOrganization_returnsOnlyUserCalendarProjection() {
                Long organizationId = 1L;
                Long userId = 10L;
                Instant from = Instant.parse("2030-01-01T00:00:00Z");
                Instant to = Instant.parse("2030-01-05T00:00:00Z");
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

                Event event = Event.builder()
                                .id(2L)
                                .organizationId(organizationId)
                                .title("My event")
                                .eventType(EventType.CONCERT)
                                .location("Big stage")
                                .startTime(from.plusSeconds(3600))
                                .endTime(from.plusSeconds(7200))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Event>>any(), eq(pageable)))
                                .thenReturn(new PageImpl<>(List.of(event), pageable, 1));
                EventSection eventSection = new EventSection();
                eventSection.setEventId(2L);
                eventSection.setSectionId(100L);
                when(eventSectionRepository.findByEventIdIn(anyCollection())).thenReturn(List.of(eventSection));

                EventUserCalendarRequestDTO request = new EventUserCalendarRequestDTO();
                request.setFrom(from);
                request.setTo(to);

                EventCalendarGroupedResponseDTO result = eventService.getCurrentUserCalendarInOrganization(
                                organizationId, request, pageable);

                assertEquals(1, result.getTotalElements());
                assertEquals(1, result.getSectionGroups().size());
                assertEquals(0, result.getOrganizationWideEvents().size());
                assertEquals(100L, result.getSectionGroups().get(0).getSectionId());
                assertEquals(1, result.getSectionGroups().get(0).getEvents().size());
                EventCalendarDTO dto = result.getSectionGroups().get(0).getEvents().get(0);
                assertEquals(2L, dto.getId());
                assertEquals("My event", dto.getTitle());
                assertEquals(EventType.CONCERT, dto.getEventType());
                assertEquals("Big stage", dto.getLocation());
        }

        @Test
        void getOrganizationEventTags_returnsUniqueSortedTags() {
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
                when(eventRepository.findDistinctTagsByOrganizationId(organizationId))
                                .thenReturn(List.of("concert", "rehearsal"));

                List<String> tags = eventService.getOrganizationEventTags(organizationId);

                assertEquals(List.of("concert", "rehearsal"), tags);
        }

        @Test
        void deleteEvent_notFound_throwsEntityNotFoundException() {
                Long eventId = 100L;

                when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class, () -> eventService.deleteEvent(1L, eventId));

                verify(eventParticipantRepository, never()).deleteByEventId(anyLong());
                verify(eventRepository, never()).deleteById(anyLong());
        }

        @Test
        void getEventAttendanceTable_success_returnsRows() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long userId1 = 10L;
                Long userId2 = 20L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));

                EventParticipant p1 = EventParticipant.builder()
                                .eventId(eventId)
                                .userId(userId1)
                                .source(EventParticipantSourceType.MANUAL)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.ATTENDED)
                                .build();

                EventParticipant p2 = EventParticipant.builder()
                                .eventId(eventId)
                                .userId(userId2)
                                .source(EventParticipantSourceType.MANUAL)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .attendanceStatus(EventAttendanceStatus.ABSENT)
                                .build();

                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of(p1, p2));

                User user1 = User.builder()
                                .id(userId1)
                                .username("user1")
                                .name("User One")
                                .email("u1@example.com")
                                .password("pwd")
                                .profileImageFileId(1L)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .notificationChannel(null)
                                .build();

                User user2 = User.builder()
                                .id(userId2)
                                .username("user2")
                                .name("User Two")
                                .email("u2@example.com")
                                .password("pwd")
                                .profileImageFileId(1L)
                                .createdAt(Instant.now())
                                .updatedAt(Instant.now())
                                .notificationChannel(null)
                                .build();

                when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user1, user2));

                List<EventAttendanceRowDTO> rows = eventService.getEventAttendanceTable(organizationId, eventId);

                assertEquals(2, rows.size());

                EventAttendanceRowDTO row1 = rows.stream()
                                .filter(r -> "User One".equals(r.getName()))
                                .findFirst()
                                .orElseThrow();
                assertEquals("User One", row1.getName());
                assertEquals(EventRsvpStatus.ACCEPTED, row1.getRsvpStatus());
                assertEquals(EventAttendanceStatus.ATTENDED, row1.getAttendanceStatus());

                EventAttendanceRowDTO row2 = rows.stream()
                                .filter(r -> "User Two".equals(r.getName()))
                                .findFirst()
                                .orElseThrow();
                assertEquals("User Two", row2.getName());
                assertEquals(EventRsvpStatus.DECLINED, row2.getRsvpStatus());
                assertEquals(EventAttendanceStatus.ABSENT, row2.getAttendanceStatus());
        }

        @Test
        void getEventAttendanceTable_eventNotFound_throwsEntityNotFoundException() {
                Long eventId = 100L;

                when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class, () -> eventService.getEventAttendanceTable(1L, eventId));

                verify(eventParticipantRepository, never()).findByEventId(anyLong());
                verify(userRepository, never()).findAllById(anyCollection());
        }

        @Test
        void getEventAttendanceTable_noParticipants_returnsEmptyList() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long currentUserId = 99L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));

                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of());

                List<EventAttendanceRowDTO> rows = eventService.getEventAttendanceTable(organizationId, eventId);

                assertEquals(0, rows.size());
                verify(userRepository, never()).findAllById(anyCollection());
        }

        @Test
        void exportCurrentUserScheduleAsIcal_excludesDeclinedWhenFlagFalse() {
                Long currentUserId = 42L;
                Long eventId1 = 100L;
                Long eventId2 = 200L;

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                EventParticipant accepted = EventParticipant.builder()
                                .eventId(eventId1)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                EventParticipant declined = EventParticipant.builder()
                                .eventId(eventId2)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByUserId(currentUserId))
                                .thenReturn(List.of(accepted, declined));

                Instant futureStart = Instant.now().plusSeconds(3600);
                Instant futureEnd = Instant.now().plusSeconds(7200);

                Event event1 = Event.builder()
                                .id(eventId1)
                                .organizationId(1L)
                                .title("Accepted Event")
                                .description("Desc 1")
                                .location("Hall 1")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(eventId2)
                                .organizationId(1L)
                                .title("Declined Event")
                                .description("Desc 2")
                                .location("Hall 2")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(event1, event2));

                String ics = eventService.exportCurrentUserScheduleAsIcal(false);

                // Должно быть базовое тело календаря
                assertTrue(ics.contains("BEGIN:VCALENDAR"));
                assertTrue(ics.contains("END:VCALENDAR"));

                // Включено событие с ACCEPTED
                assertTrue(ics.contains("SUMMARY:Accepted Event"));
                // Исключено событие с DECLINED
                assertFalse(ics.contains("SUMMARY:Declined Event"));
        }

        @Test
        void exportCurrentUserScheduleAsIcal_includesDeclinedWhenFlagTrue() {
                Long currentUserId = 42L;
                Long eventId1 = 100L;
                Long eventId2 = 200L;

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                EventParticipant accepted = EventParticipant.builder()
                                .eventId(eventId1)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                EventParticipant declined = EventParticipant.builder()
                                .eventId(eventId2)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByUserId(currentUserId))
                                .thenReturn(List.of(accepted, declined));

                Instant futureStart = Instant.now().plusSeconds(3600);
                Instant futureEnd = Instant.now().plusSeconds(7200);

                Event event1 = Event.builder()
                                .id(eventId1)
                                .organizationId(1L)
                                .title("Accepted Event")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(eventId2)
                                .organizationId(1L)
                                .title("Declined Event")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(event1, event2));

                String ics = eventService.exportCurrentUserScheduleAsIcal(true);

                assertTrue(ics.contains("SUMMARY:Accepted Event"));
                assertTrue(ics.contains("SUMMARY:Declined Event"));
        }

        @Test
        void exportCurrentUserScheduleAsIcal_generatesUniqueUidForEventsWithSameTitle() {
                Long currentUserId = 42L;
                Long eventId1 = 953L;
                Long eventId2 = 954L;

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                EventParticipant participant1 = EventParticipant.builder()
                                .eventId(eventId1)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                EventParticipant participant2 = EventParticipant.builder()
                                .eventId(eventId2)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByUserId(currentUserId))
                                .thenReturn(List.of(participant1, participant2));

                Instant now = Instant.now();
                Instant start1 = now.plusSeconds(3600);
                Instant end1 = now.plusSeconds(7200);
                Instant start2 = now.plusSeconds(10800);
                Instant end2 = now.plusSeconds(14400);

                Event event1 = Event.builder()
                                .id(eventId1)
                                .organizationId(1L)
                                .title("Event 953")
                                .description("Event description")
                                .startTime(start1)
                                .endTime(end1)
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(eventId2)
                                .organizationId(1L)
                                .title("Event 953")
                                .description("Event description")
                                .startTime(start2)
                                .endTime(end2)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(event1, event2));

                String ics = eventService.exportCurrentUserScheduleAsIcal(true);

                assertTrue(ics.contains("UID:event-953@orkestro"));
                assertTrue(ics.contains("UID:event-954@orkestro"));
        }

        @Test
        void exportCurrentUserScheduleAsCsv_excludesDeclinedWhenFlagFalse() {
                Long currentUserId = 42L;
                Long eventId1 = 100L;
                Long eventId2 = 200L;

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                EventParticipant accepted = EventParticipant.builder()
                                .eventId(eventId1)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                EventParticipant declined = EventParticipant.builder()
                                .eventId(eventId2)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByUserId(currentUserId))
                                .thenReturn(List.of(accepted, declined));

                Instant futureStart = Instant.now().plusSeconds(3600);
                Instant futureEnd = Instant.now().plusSeconds(7200);

                Event event1 = Event.builder()
                                .id(eventId1)
                                .organizationId(1L)
                                .title("Accepted Event")
                                .description("Desc 1")
                                .location("Hall 1")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(eventId2)
                                .organizationId(1L)
                                .title("Declined Event")
                                .description("Desc 2")
                                .location("Hall 2")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(event1, event2));

                String csv = eventService.exportCurrentUserScheduleAsCsv(false);

                // header
                assertTrue(csv.startsWith(
                                "title,organization_id,start_time_utc,end_time_utc,location,rsvp_status"));
                // contains accepted event
                assertTrue(csv.contains("Accepted Event"));
                // does not contain declined event
                assertFalse(csv.contains("Declined Event"));
        }

        @Test
        void exportCurrentUserScheduleAsCsv_includesDeclinedWhenFlagTrue() {
                Long currentUserId = 42L;
                Long eventId1 = 100L;
                Long eventId2 = 200L;

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);

                EventParticipant accepted = EventParticipant.builder()
                                .eventId(eventId1)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                EventParticipant declined = EventParticipant.builder()
                                .eventId(eventId2)
                                .userId(currentUserId)
                                .rsvpStatus(EventRsvpStatus.DECLINED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByUserId(currentUserId))
                                .thenReturn(List.of(accepted, declined));

                Instant futureStart = Instant.now().plusSeconds(3600);
                Instant futureEnd = Instant.now().plusSeconds(7200);

                Event event1 = Event.builder()
                                .id(eventId1)
                                .organizationId(1L)
                                .title("Accepted Event")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(eventId2)
                                .organizationId(1L)
                                .title("Declined Event")
                                .startTime(futureStart)
                                .endTime(futureEnd)
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(event1, event2));

                String csv = eventService.exportCurrentUserScheduleAsCsv(true);

                assertTrue(csv.contains("Accepted Event"));
                assertTrue(csv.contains("Declined Event"));
        }

        @Test
        void markEventAttendance_success_updatesAttendanceStatus() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long participantUserId = 10L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));

                EventParticipant participant = EventParticipant.builder()
                                .eventId(eventId)
                                .userId(participantUserId)
                                .source(EventParticipantSourceType.MANUAL)
                                .rsvpStatus(EventRsvpStatus.ACCEPTED)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .build();

                when(eventParticipantRepository.findByEventIdAndUserId(eventId, participantUserId))
                                .thenReturn(Optional.of(participant));

                eventService.markEventAttendance(organizationId, eventId, participantUserId, EventAttendanceStatus.ATTENDED);

                ArgumentCaptor<EventParticipant> participantCaptor = ArgumentCaptor.forClass(EventParticipant.class);
                verify(eventParticipantRepository).save(participantCaptor.capture());
                EventParticipant saved = participantCaptor.getValue();

                assertEquals(EventAttendanceStatus.ATTENDED, saved.getAttendanceStatus());
                assertEquals(eventId, saved.getEventId());
                assertEquals(participantUserId, saved.getUserId());
        }

        @Test
        void markEventAttendance_eventNotFound_throwsEntityNotFoundException() {
                Long eventId = 100L;

                when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class,
                                () -> eventService.markEventAttendance(1L, eventId, 10L, EventAttendanceStatus.ATTENDED));

                verify(eventParticipantRepository, never()).findByEventIdAndUserId(anyLong(), anyLong());
                verify(eventParticipantRepository, never()).save(any());
        }

        @Test
        void markEventAttendance_participantNotFound_throwsEntityNotFoundException() {
                Long eventId = 100L;
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Long participantUserId = 10L;

                Event event = Event.builder()
                                .id(eventId)
                                .organizationId(organizationId)
                                .title("Event")
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));

                when(eventParticipantRepository.findByEventIdAndUserId(eventId, participantUserId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                EntityNotFoundException.class,
                                () -> eventService.markEventAttendance(
                                                organizationId, eventId, participantUserId, EventAttendanceStatus.ATTENDED));

                verify(eventParticipantRepository, never()).save(any());
        }

        @Test
        void attachFileToEvent_whenAlreadyHas50Files_throwsBusinessException() {
                Long organizationId = 1L;
                Long eventId = 100L;
                Long currentUserId = 99L;

                Event event = Event.builder().id(eventId).organizationId(organizationId).build();
                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));

                List<EventFile> files = java.util.stream.IntStream.range(0, 50)
                                .mapToObj(i -> {
                                        EventFile f = new EventFile();
                                        f.setEventId(eventId);
                                        f.setFileId((long) i + 1);
                                        return f;
                                })
                                .toList();
                when(eventFileRepository.findByEventId(eventId)).thenReturn(files);

                MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());
                assertThrows(BusinessException.class, () -> eventService.attachFileToEvent(organizationId, eventId, file));
                verify(fileStorageService, never()).uploadForCurrentUser(any(), any());
        }

        @Test
        void deleteEventFile_whenFileStillReferenced_doesNotDeletePhysicalFile() {
                Long organizationId = 1L;
                Long eventId = 100L;
                Long currentUserId = 99L;
                Long fileId = 700L;

                Event event = Event.builder().id(eventId).organizationId(organizationId).build();
                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(currentUserId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(membership));
                when(eventFileRepository.existsByEventIdAndFileId(eventId, fileId)).thenReturn(true);
                when(fileReferenceService.isFileReferenced(fileId)).thenReturn(true);
                when(eventMapper.toDto(any(Event.class))).thenReturn(EventDTO.builder().id(eventId).organizationId(organizationId).build());
                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of());
                when(eventFileRepository.findByEventId(eventId)).thenReturn(List.of());

                eventService.deleteEventFile(organizationId, eventId, fileId);

                verify(eventFileRepository).deleteByEventIdAndFileId(eventId, fileId);
                verify(fileStorageService, never()).delete(fileId);
        }

        @Test
        void createEventComment_success_forCreator() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long creatorUserId = 99L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(creatorUserId).build();
                EventCommentCreateRequestDTO request = EventCommentCreateRequestDTO.builder().text("  Progress is good  ").build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(creatorUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, creatorUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(creatorUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(eventCommentRepository.countByEventId(eventId)).thenReturn(0L);
                when(eventCommentRepository.save(any(EventComment.class))).thenAnswer(invocation -> {
                        EventComment comment = invocation.getArgument(0);
                        comment.setId(1L);
                        comment.setCreatedAt(Instant.parse("2026-02-27T10:00:00Z"));
                        return comment;
                });
                when(userRepository.findById(creatorUserId))
                                .thenReturn(Optional.of(User.builder().id(creatorUserId).name("Creator").build()));

                EventCommentDTO result = eventService.createEventComment(organizationId, eventId, request);

                assertEquals(1L, result.getId());
                assertEquals("Progress is good", result.getText());
                assertEquals("Creator", result.getAuthorName());
        }

        @Test
        void createEventComment_success_forUserWithPermission() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long currentUserId = 100L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(99L).build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(currentUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(organizationPermissionChecker.hasOrganizationPermission(organizationId, "EVENT_WRITE_COMMENT"))
                                .thenReturn(true);
                when(eventCommentRepository.countByEventId(eventId)).thenReturn(1L);
                when(eventCommentRepository.save(any(EventComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

                eventService.createEventComment(
                                organizationId, eventId, EventCommentCreateRequestDTO.builder().text("ok").build());

                verify(eventCommentRepository).save(any(EventComment.class));
        }

        @Test
        void createEventComment_withoutPermissionAndNotCreator_throwsBusinessException() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long currentUserId = 100L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(99L).build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(currentUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(organizationPermissionChecker.hasOrganizationPermission(organizationId, "EVENT_WRITE_COMMENT"))
                                .thenReturn(false);

                assertThrows(
                                BusinessException.class,
                                () -> eventService.createEventComment(
                                                organizationId, eventId, EventCommentCreateRequestDTO.builder().text("Denied").build()));
                verify(eventCommentRepository, never()).save(any());
        }

        @Test
        void createEventComment_limitReached_throwsBusinessException() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long creatorUserId = 99L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(creatorUserId).build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(creatorUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, creatorUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(creatorUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(eventCommentRepository.countByEventId(eventId)).thenReturn(10L);

                assertThrows(
                                BusinessException.class,
                                () -> eventService.createEventComment(
                                                organizationId, eventId, EventCommentCreateRequestDTO.builder().text("11th").build()));
        }

        @Test
        void createEventComment_notifiesAllParticipantsExceptAuthor() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long authorUserId = 99L;
                Long participant1 = 100L;
                Long participant2 = 101L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(authorUserId).build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(authorUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, authorUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(authorUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(eventCommentRepository.countByEventId(eventId)).thenReturn(0L);
                when(eventCommentRepository.save(any(EventComment.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(userRepository.findById(authorUserId))
                                .thenReturn(Optional.of(User.builder().id(authorUserId).name("Author").build()));
                when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of(
                                EventParticipant.builder().eventId(eventId).userId(authorUserId).build(),
                                EventParticipant.builder().eventId(eventId).userId(participant1).build(),
                                EventParticipant.builder().eventId(eventId).userId(participant2).build()));

                eventService.createEventComment(
                                organizationId, eventId, EventCommentCreateRequestDTO.builder().text("Comment").build());

                verify(eventNotificationService).sendEventCommentNotifications(
                                eq(event), eq(List.of(participant1, participant2)), eq("Author"), eq("Comment"));
        }

        @Test
        void createEventComment_notificationFailure_doesNotBreakCommentCreation() {
                Long organizationId = 1L;
                Long eventId = 10L;
                Long authorUserId = 99L;
                Event event = Event.builder().id(eventId).organizationId(organizationId).creatorUserId(authorUserId).build();

                when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
                when(securityUtils.getCurrentUserId()).thenReturn(authorUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, authorUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(authorUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));
                when(eventCommentRepository.countByEventId(eventId)).thenReturn(0L);
                when(eventCommentRepository.save(any(EventComment.class))).thenAnswer(invocation -> {
                        EventComment c = invocation.getArgument(0);
                        c.setId(123L);
                        return c;
                });
                when(userRepository.findById(authorUserId))
                                .thenReturn(Optional.of(User.builder().id(authorUserId).name("Author").build()));
                when(eventParticipantRepository.findByEventId(eventId))
                                .thenReturn(List.of(EventParticipant.builder().eventId(eventId).userId(100L).build()));
                org.mockito.Mockito.doThrow(new RuntimeException("notification transport down"))
                                .when(eventNotificationService)
                                .sendEventCommentNotifications(eq(event), anyCollection(), eq("Author"), eq("Comment"));

                EventCommentDTO result = eventService.createEventComment(
                                organizationId, eventId, EventCommentCreateRequestDTO.builder().text("Comment").build());

                assertEquals(123L, result.getId());
                assertEquals("Comment", result.getText());
        }

        @Test
        void getEventCommentsByEventIds_returnsGroupedByEventsPage() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                List<Long> eventIds = List.of(11L, 12L, 13L);
                Pageable pageable = PageRequest.of(0, 2);

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(currentUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));

                Event e11 = Event.builder().id(11L).organizationId(organizationId).build();
                Event e12 = Event.builder().id(12L).organizationId(organizationId).build();
                when(eventRepository.findAllById(List.of(11L, 12L))).thenReturn(List.of(e11, e12));

                EventComment c1 = EventComment.builder().id(1L).eventId(11L).authorUserId(500L).text("A").createdAt(Instant.now()).build();
                EventComment c2 = EventComment.builder().id(2L).eventId(11L).authorUserId(501L).text("B").createdAt(Instant.now()).build();
                when(eventCommentRepository.findByEventIdInOrderByCreatedAtDesc(List.of(11L, 12L))).thenReturn(List.of(c1, c2));
                when(userRepository.findAllById(anyCollection()))
                                .thenReturn(List.of(
                                                User.builder().id(500L).name("U500").build(),
                                                User.builder().id(501L).name("U501").build()));

                EventCommentsByEventPageDTO result = eventService.getEventCommentsByEventIds(organizationId, eventIds, pageable);

                assertEquals(3, result.getTotalElements());
                assertEquals(2, result.getTotalPages());
                assertEquals(2, result.getContent().size());
                assertEquals(11L, result.getContent().get(0).getEventId());
                assertEquals(2, result.getContent().get(0).getCommentsCount());
                assertEquals(12L, result.getContent().get(1).getEventId());
                assertEquals(0, result.getContent().get(1).getCommentsCount());
        }

        @Test
        void getEventCommentsByEventIds_wrongOrganization_throwsBusinessException() {
                Long organizationId = 1L;
                Long currentUserId = 99L;
                Pageable pageable = PageRequest.of(0, 20);

                when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, currentUserId))
                                .thenReturn(Optional.of(OrganizationUser.builder()
                                                .organizationId(organizationId)
                                                .userId(currentUserId)
                                                .status(OrganizationUserStatusType.ACCEPTED)
                                                .joinedAt(Instant.now())
                                                .build()));

                Event foreignEvent = Event.builder().id(99L).organizationId(5L).build();
                when(eventRepository.findAllById(List.of(99L))).thenReturn(List.of(foreignEvent));

                assertThrows(
                                BusinessException.class,
                                () -> eventService.getEventCommentsByEventIds(organizationId, List.of(99L), pageable));
        }
}
