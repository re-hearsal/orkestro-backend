package io.github.Romariok.orkestro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.mapper.EventMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventFile;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventParticipantSourceType;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.models.enums.EventType;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.event.repository.EventSongRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.service.EventService;
import io.github.Romariok.orkestro.event.service.EventNotificationService;
import io.github.Romariok.orkestro.organization.models.Organization;
import io.github.Romariok.orkestro.organization.models.OrganizationUser;
import io.github.Romariok.orkestro.organization.models.enums.OrganizationUserStatusType;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationUserRepository;
import io.github.Romariok.orkestro.security.SecurityUtils;
import io.github.Romariok.orkestro.section.models.Section;
import io.github.Romariok.orkestro.section.models.SectionUser;
import io.github.Romariok.orkestro.section.repository.SectionRepository;
import io.github.Romariok.orkestro.section.repository.SectionUserRepository;
import io.github.Romariok.orkestro.user.models.User;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

        @Mock
        private EventRepository eventRepository;

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
        private EventFileRepository eventFileRepository;

        @Mock
        private StoredFileRepository storedFileRepository;

        @Mock
        private EventSongRepository eventSongRepository;

        @InjectMocks
        private EventService eventService;

        @Mock
        private EventNotificationService eventNotificationService;

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
                                .fileIds(List.of(1000L, 2000L))
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

                StoredFile file1 = StoredFile.builder().id(1000L).name("f1").fileType(null).bucketName("b")
                                .objectName("o1").size(1L).createdAt(Instant.now()).uploadedByUserId(currentUserId)
                                .build();
                StoredFile file2 = StoredFile.builder().id(2000L).name("f2").fileType(null).bucketName("b")
                                .objectName("o2").size(1L).createdAt(Instant.now()).uploadedByUserId(currentUserId)
                                .build();
                when(storedFileRepository.findAllById(anyCollection())).thenReturn(List.of(file1, file2));

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
                EventFile ef1 = new EventFile();
                ef1.setEventId(100L);
                ef1.setFileId(1000L);
                EventFile ef2 = new EventFile();
                ef2.setEventId(100L);
                ef2.setFileId(2000L);
                when(eventFileRepository.findByEventId(100L)).thenReturn(List.of(ef1, ef2));

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

                assertThrows(BusinessException.class, () -> eventService.getEventForCurrentUser(eventId));
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

                EventDTO dto = eventService.getEventForCurrentUser(eventId);

                assertEquals(eventId, dto.getId());
                assertEquals(organizationId, dto.getOrganizationId());
                assertEquals("Event", dto.getTitle());
        }

        @Test
        void searchEventsForCurrentUserInOrganization_filtersByTitleAndTags() {
                Long organizationId = 1L;
                Long userId = 10L;

                // ensure user is member of organization
                when(securityUtils.getCurrentUserId()).thenReturn(userId);
                OrganizationUser membership = OrganizationUser.builder()
                                .organizationId(organizationId)
                                .userId(userId)
                                .status(OrganizationUserStatusType.ACCEPTED)
                                .joinedAt(Instant.now())
                                .build();
                when(organizationUserRepository.findByOrganizationIdAndUserId(organizationId, userId))
                                .thenReturn(Optional.of(membership));

                // three events in organization with different titles/tags
                Event event1 = Event.builder()
                                .id(1L)
                                .organizationId(organizationId)
                                .title("Morning rehearsal")
                                .tags(Set.of("strings", "morning"))
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                Event event2 = Event.builder()
                                .id(2L)
                                .organizationId(organizationId)
                                .title("Evening rehearsal")
                                .tags(Set.of("strings", "evening"))
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                Event event3 = Event.builder()
                                .id(3L)
                                .organizationId(organizationId)
                                .title("Morning meeting")
                                .tags(Set.of("management", "morning"))
                                .startTime(Instant.now())
                                .endTime(Instant.now().plusSeconds(3600))
                                .createdAt(Instant.now())
                                .build();

                when(eventRepository.findByOrganizationId(organizationId))
                                .thenReturn(List.of(event1, event2, event3));

                // no participants required for search logic
                when(eventParticipantRepository.findByEventIdIn(anyCollection())).thenReturn(List.of());

                // mapstruct mapper stub: map events list to DTO list one-to-one
                when(eventMapper.toDtoList(anyList())).thenAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        List<Event> events = invocation.getArgument(0, List.class);
                        return events.stream()
                                        .map(e -> EventDTO.builder()
                                                        .id(e.getId())
                                                        .organizationId(e.getOrganizationId())
                                                        .title(e.getTitle())
                                                        .tags(e.getTags() == null ? List.of()
                                                                        : e.getTags().stream().toList())
                                                        .startTime(e.getStartTime())
                                                        .endTime(e.getEndTime())
                                                        .createdAt(e.getCreatedAt())
                                                        .build())
                                        .toList();
                });

                // search by title substring "rehearsal" and tag "morning"
                List<EventDTO> result = eventService.searchEventsForCurrentUserInOrganization(
                                organizationId, "rehearsal", List.of("morning"));

                assertEquals(1, result.size());
                EventDTO dto = result.get(0);
                assertEquals(1L, dto.getId());
                assertEquals("Morning rehearsal", dto.getTitle());
                assertEquals(
                                Set.of("strings", "morning"),
                                dto.getTags() == null ? Set.of() : Set.copyOf(dto.getTags()));
        }

        @Test
        void deleteEvent_notFound_throwsEntityNotFoundException() {
                Long eventId = 100L;

                when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

                assertThrows(EntityNotFoundException.class, () -> eventService.deleteEvent(eventId));

                verify(eventParticipantRepository, never()).deleteByEventId(anyLong());
                verify(eventRepository, never()).deleteById(anyLong());
        }
}
