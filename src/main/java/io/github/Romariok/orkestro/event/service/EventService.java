package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarGroupedResponseDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCalendarScope;
import io.github.Romariok.orkestro.event.dto.EventCalendarSectionGroupDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventDTO;
import io.github.Romariok.orkestro.event.dto.EventCommentsByEventPageDTO;
import io.github.Romariok.orkestro.event.models.EventDescriptionTemplate;
import io.github.Romariok.orkestro.event.repository.EventDescriptionTemplateRepository;
import io.github.Romariok.orkestro.event.dto.EventUserCalendarRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.mapper.EventMapper;
import io.github.Romariok.orkestro.event.models.EventComment;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventFile;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventSection;
import io.github.Romariok.orkestro.event.models.EventSong;
import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventParticipantSourceType;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventCommentRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.repository.EventSectionRepository;
import io.github.Romariok.orkestro.event.repository.EventSongRepository;
import io.github.Romariok.orkestro.event.specification.EventSpecifications;
import io.github.Romariok.orkestro.config.FileLimitsProperties;
import io.github.Romariok.orkestro.repertoire.models.Song;
import io.github.Romariok.orkestro.repertoire.repository.SongRepository;
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
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileTypeDetector;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import io.github.Romariok.orkestro.notification.WebSocketNotificationService;
import io.github.Romariok.orkestro.notification.dto.InAppNotificationDTO;
import io.github.Romariok.orkestro.notification.models.enums.InAppNotificationType;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventCommentRepository eventCommentRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventFileRepository eventFileRepository;
    private final EventSongRepository eventSongRepository;
    private final EventSectionRepository eventSectionRepository;
    private final SongRepository songRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final SectionRepository sectionRepository;
    private final SectionUserRepository sectionUserRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final SecurityUtils securityUtils;
    private final FileStorageService fileStorageService;
    private final FileRollbackHelper fileRollbackHelper;
    private final EventNotificationService eventNotificationService;
    private final FileReferenceService fileReferenceService;
    private final FileLimitsProperties fileLimitsProperties;
    private final OrganizationPermissionChecker organizationPermissionChecker;
    private final EventDescriptionTemplateRepository eventDescriptionTemplateRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    private static final int CALENDAR_MAX_PAGE_SIZE = 500;
    private static final Duration CALENDAR_DEFAULT_PAST_WINDOW = Duration.ofDays(7);
    private static final Duration CALENDAR_DEFAULT_FUTURE_WINDOW = Duration.ofDays(42);
    private static final Duration CALENDAR_MAX_WINDOW = Duration.ofDays(92);
    private static final int EVENT_COMMENTS_MAX_PER_EVENT = 10;
    private static final int EVENT_COMMENTS_MAX_LENGTH = 3000;

    @Transactional
    public EventDTO createEventInOrganization(Long organizationId, EventCreateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }

        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + organizationId));

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        Instant now = Instant.now();

        validateSongsForOrganization(organizationId, request.getSongIds());

        boolean includeAll = Boolean.TRUE.equals(request.getIncludeAllOrganizationMembers());
        List<Long> participantSectionIds = normalizeAndValidateParticipantSectionIds(
                organizationId, request.getParticipantSectionIds(), includeAll);
        Map<Long, EventParticipantSourceType> sources = buildParticipantSources(
                organizationId, request.getParticipantUserIds(), participantSectionIds, includeAll);

        Set<String> tags = request.getTags() != null ? sanitizeTags(request.getTags()) : null;

        boolean sendRsvp = Boolean.TRUE.equals(request.getSendRsvp());
        Integer remindBeforeMinutes = request.getRemindBeforeMinutes();

        String description = request.getDescription();
        if (request.getDescriptionTemplateId() != null) {
            EventDescriptionTemplate template = eventDescriptionTemplateRepository
                    .findById(request.getDescriptionTemplateId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Description template not found: " + request.getDescriptionTemplateId()));
            if (!template.getOrganizationId().equals(organizationId)) {
                throw new BusinessException("Description template does not belong to organization " + organizationId);
            }
            description = template.getContent();
        }

        Event event = Event.builder()
                .organizationId(organizationId)
                .creatorUserId(currentUserId)
                .title(request.getTitle().trim())
                .description(description)
                .eventType(request.getEventType())
                .externalLink(request.getExternalLink())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .sendRsvp(sendRsvp)
                .remindBeforeMinutes(remindBeforeMinutes)
                .includeAllOrganizationMembers(includeAll)
                .createdAt(now)
                .tags(tags)
                .build();

        List<Long> uploadedFileIds = List.of();
        try {
            uploadedFileIds = uploadEventFiles(request.getFiles());

            Event saved = eventRepository.save(event);

            saveParticipants(saved.getId(), sources);
            saveEventSections(saved.getId(), participantSectionIds, includeAll);
            saveEventFiles(saved.getId(), uploadedFileIds);
            saveEventSongs(saved.getId(), request.getSongIds());

            if (sendRsvp) {
                eventNotificationService.sendEventCreatedNotifications(saved, sources.keySet());
            }

            for (Long participantId : sources.keySet()) {
                if (!participantId.equals(currentUserId)) {
                    try {
                        webSocketNotificationService.send(participantId, InAppNotificationDTO.builder()
                                .type(InAppNotificationType.NEW_EVENT)
                                .title("New event: " + saved.getTitle())
                                .body(saved.getDescription())
                                .entityId(saved.getId())
                                .entityType("EVENT")
                                .build());
                    } catch (Exception ex) {
                        log.warn("Failed to send WebSocket notification for event {} to user {}", saved.getId(), participantId, ex);
                    }
                }
            }

            return buildEventDto(saved);
        } catch (RuntimeException ex) {
            fileRollbackHelper.deleteFilesSafely(uploadedFileIds);
            throw ex;
        }
    }

    @Transactional
    public EventDTO updateEvent(Long organizationId, Long eventId, EventUpdateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Event title must not be blank");
            }
            event.setTitle(title);
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getEventType() != null) {
            event.setEventType(request.getEventType());
        }

        if (request.getExternalLink() != null) {
            event.setExternalLink(request.getExternalLink());
        }

        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }

        if (request.getRemindBeforeMinutes() != null) {
            event.setRemindBeforeMinutes(request.getRemindBeforeMinutes());
        }

        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }

        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }

        if (event.getStartTime() == null || event.getEndTime() == null) {
            throw new IllegalStateException("Event start and end time must not be null");
        }

        if (!event.getEndTime().isAfter(event.getStartTime())) {
            throw new IllegalArgumentException("Event end time must be after start time");
        }

        if (request.getTags() != null) {
            Set<String> tags = sanitizeTags(request.getTags());
            event.setTags(tags);
        }
        if (request.getIncludeAllOrganizationMembers() != null) {
            event.setIncludeAllOrganizationMembers(Boolean.TRUE.equals(request.getIncludeAllOrganizationMembers()));
        }
        boolean participantsChanged = request.getParticipantUserIds() != null
                || request.getParticipantSectionIds() != null
                || request.getIncludeAllOrganizationMembers() != null;
        if (participantsChanged) {
            boolean includeAll = event.isIncludeAllOrganizationMembers();
            List<Long> participantSectionIds = request.getParticipantSectionIds();
            if (participantSectionIds == null) {
                participantSectionIds = eventSectionRepository.findByEventId(eventId).stream()
                        .map(EventSection::getSectionId)
                        .toList();
            }
            participantSectionIds = normalizeAndValidateParticipantSectionIds(
                    event.getOrganizationId(), participantSectionIds, includeAll);
            Map<Long, EventParticipantSourceType> sources = buildParticipantSources(
                    event.getOrganizationId(),
                    request.getParticipantUserIds(),
                    participantSectionIds,
                    includeAll);

            eventParticipantRepository.deleteByEventId(eventId);
            saveParticipants(eventId, sources);
            eventSectionRepository.deleteByEventId(eventId);
            saveEventSections(eventId, participantSectionIds, includeAll);
        }

        if (request.getSongIds() != null) {
            validateSongsForOrganization(event.getOrganizationId(), request.getSongIds());
            eventSongRepository.deleteByEventId(eventId);
            saveEventSongs(eventId, request.getSongIds());
        }

        Event saved = eventRepository.save(event);
        return buildEventDto(saved);
    }

    @Transactional
    public List<EventDTO> duplicateEvent(Long organizationId, Long eventId, List<Instant> startTimes) {
        if (startTimes == null || startTimes.isEmpty()) {
            throw new IllegalArgumentException("startTimes must not be empty");
        }

        Event source = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(source, organizationId);
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(source.getOrganizationId(), currentUserId);

        if (source.getStartTime() == null || source.getEndTime() == null
                || !source.getEndTime().isAfter(source.getStartTime())) {
            throw new IllegalStateException("Source event has invalid time range");
        }
        long durationSeconds = source.getEndTime().getEpochSecond() - source.getStartTime().getEpochSecond();

        List<EventParticipant> sourceParticipants = eventParticipantRepository.findByEventId(eventId);
        List<EventFile> sourceFiles = eventFileRepository.findByEventId(eventId);
        List<EventSong> sourceSongs = eventSongRepository.findByEventId(eventId);
        List<EventSection> sourceSections = eventSectionRepository.findByEventId(eventId);

        List<EventDTO> result = new ArrayList<>();
        for (Instant duplicatedStartTime : startTimes) {
            if (duplicatedStartTime == null) {
                throw new IllegalArgumentException("startTimes must not contain null values");
            }
            Instant duplicatedEndTime = duplicatedStartTime.plusSeconds(durationSeconds);
            Event duplicated = Event.builder()
                    .organizationId(source.getOrganizationId())
                    .creatorUserId(currentUserId)
                    .title(source.getTitle())
                    .description(source.getDescription())
                    .eventType(source.getEventType())
                    .externalLink(source.getExternalLink())
                    .location(source.getLocation())
                    .startTime(duplicatedStartTime)
                    .endTime(duplicatedEndTime)
                    .sendRsvp(source.isSendRsvp())
                    .remindBeforeMinutes(source.getRemindBeforeMinutes())
                    .includeAllOrganizationMembers(source.isIncludeAllOrganizationMembers())
                    .tags(source.getTags() != null ? new HashSet<>(source.getTags()) : new HashSet<>())
                    .build();

            Event saved = eventRepository.save(duplicated);
            Long newEventId = saved.getId();

            if (!sourceParticipants.isEmpty()) {
                List<EventParticipant> duplicatedParticipants = sourceParticipants.stream()
                        .map(p -> EventParticipant.builder()
                                .eventId(newEventId)
                                .userId(p.getUserId())
                                .source(p.getSource())
                                .rsvpStatus(EventRsvpStatus.PENDING)
                                .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                                .rsvpAt(null)
                                .build())
                        .toList();
                eventParticipantRepository.saveAll(duplicatedParticipants);
            }

            if (!sourceFiles.isEmpty()) {
                List<EventFile> duplicatedFiles = sourceFiles.stream()
                        .map(f -> {
                            EventFile ef = new EventFile();
                            ef.setEventId(newEventId);
                            ef.setFileId(f.getFileId());
                            return ef;
                        })
                        .toList();
                eventFileRepository.saveAll(duplicatedFiles);
            }

            if (!sourceSongs.isEmpty()) {
                List<EventSong> duplicatedSongs = sourceSongs.stream()
                        .map(s -> {
                            EventSong es = new EventSong();
                            es.setEventId(newEventId);
                            es.setSongId(s.getSongId());
                            es.setPosition(s.getPosition());
                            return es;
                        })
                        .toList();
                eventSongRepository.saveAll(duplicatedSongs);
            }
            if (!sourceSections.isEmpty() && !source.isIncludeAllOrganizationMembers()) {
                List<EventSection> duplicatedSections = sourceSections.stream()
                        .map(s -> {
                            EventSection es = new EventSection();
                            es.setEventId(newEventId);
                            es.setSectionId(s.getSectionId());
                            return es;
                        })
                        .toList();
                eventSectionRepository.saveAll(duplicatedSections);
            }

            result.add(buildEventDto(saved));
        }
        return result;
    }

    @Transactional
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission("
            + "@eventRepository.findById(#eventId).orElse(null)?.organizationId, 'EVENT_DELETION')")
    public void deleteEvent(Long organizationId, Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        eventParticipantRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }

    @Transactional
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission("
            + "@eventRepository.findById(#eventId).orElse(null)?.organizationId, 'EVENT_MARK_ATTENDANCE')")
    public void markEventAttendance(
            Long organizationId, Long eventId, Long participantUserId, EventAttendanceStatus attendanceStatus) {
        if (attendanceStatus == null) {
            throw new IllegalArgumentException("Attendance status must not be null");
        }

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndUserId(eventId, participantUserId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Event participant not found for event " + eventId + " and user " + participantUserId));

        participant.setAttendanceStatus(attendanceStatus);
        eventParticipantRepository.save(participant);
    }

    @Transactional
    public EventDTO attachFileToEvent(Long organizationId, Long eventId, MultipartFile file) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        if (!organizationId.equals(event.getOrganizationId())) {
            throw new BusinessException("Event " + eventId + " does not belong to organization " + organizationId);
        }

        List<EventFile> currentFiles = eventFileRepository.findByEventId(eventId);
        if (currentFiles.size() >= fileLimitsProperties.getEventMaxFiles()) {
            throw new BusinessException("Event files limit reached (" + fileLimitsProperties.getEventMaxFiles() + ")");
        }

        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new IllegalArgumentException("file is required");
        }

        StoredFile stored = fileStorageService.uploadForCurrentUser(file, FileTypeDetector.detect(file));
        Long fileId = stored.getId();
        try {
            if (!eventFileRepository.existsByEventIdAndFileId(eventId, fileId)) {
                EventFile eventFile = new EventFile();
                eventFile.setEventId(eventId);
                eventFile.setFileId(fileId);
                eventFileRepository.save(eventFile);
            }
            return buildEventDto(event);
        } catch (RuntimeException ex) {
            fileRollbackHelper.deleteFilesSafely(List.of(fileId));
            throw ex;
        }
    }

    @Transactional
    public EventDTO deleteEventFile(Long organizationId, Long eventId, Long fileId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        if (!organizationId.equals(event.getOrganizationId())) {
            throw new BusinessException("Event " + eventId + " does not belong to organization " + organizationId);
        }

        if (!eventFileRepository.existsByEventIdAndFileId(eventId, fileId)) {
            throw new EntityNotFoundException("File " + fileId + " is not attached to event " + eventId);
        }

        eventFileRepository.deleteByEventIdAndFileId(eventId, fileId);
        if (!fileReferenceService.isFileReferenced(fileId)) {
            fileStorageService.delete(fileId);
        }
        return buildEventDto(event);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission("
            + "@eventRepository.findById(#eventId).orElse(null)?.organizationId, 'EVENT_MARK_ATTENDANCE')")
    public List<EventAttendanceRowDTO> getEventAttendanceTable(Long organizationId, Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        List<EventParticipant> participants = eventParticipantRepository.findByEventId(eventId);
        if (participants.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = participants.stream()
                .map(EventParticipant::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(
                        User::getId,
                        u -> u));

        return participants.stream()
                .map(p -> {
                    User user = usersById.get(p.getUserId());
                    String name = user != null ? user.getName() : null;

                    return EventAttendanceRowDTO.builder()
                            .name(name)
                            .rsvpStatus(p.getRsvpStatus())
                            .attendanceStatus(p.getAttendanceStatus())
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission("
            + "@eventRepository.findById(#eventId).orElse(null)?.organizationId, 'EVENT_MARK_ATTENDANCE')")
    public String exportEventAttendanceMatrixAsCsv(Long organizationId, Long eventId) {
        List<EventAttendanceRowDTO> rows = getEventAttendanceTable(organizationId, eventId);
        StringBuilder sb = new StringBuilder();
        sb.append("name,rsvp_status,attendance_status\n");
        for (EventAttendanceRowDTO row : rows) {
            sb.append(escapeCsv(row.getName())).append(',');
            sb.append(row.getRsvpStatus() != null ? row.getRsvpStatus().name() : "").append(',');
            sb.append(row.getAttendanceStatus() != null ? row.getAttendanceStatus().name() : "").append('\n');
        }
        return sb.toString();
    }

    @Transactional
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_WRITE_COMMENT')")
    public EventCommentDTO createEventComment(
            Long organizationId, Long eventId, EventCommentCreateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        String rawText = request.getText();
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment text must not be blank");
        }
        String normalizedText = rawText.trim();
        if (normalizedText.length() > EVENT_COMMENTS_MAX_LENGTH) {
            throw new IllegalArgumentException("Comment text length must be <= " + EVENT_COMMENTS_MAX_LENGTH);
        }

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        boolean isCreator = currentUserId.equals(event.getCreatorUserId());
        boolean hasPermission = organizationPermissionChecker.hasOrganizationPermission(organizationId, "EVENT_WRITE_COMMENT");
        if (!isCreator && !hasPermission) {
            throw new BusinessException("You do not have permission to comment on this event");
        }

        long existingCount = eventCommentRepository.countByEventId(eventId);
        if (existingCount >= EVENT_COMMENTS_MAX_PER_EVENT) {
            throw new BusinessException("Event comments limit reached (" + EVENT_COMMENTS_MAX_PER_EVENT + ")");
        }

        EventComment saved = eventCommentRepository.save(EventComment.builder()
                .eventId(eventId)
                .authorUserId(currentUserId)
                .text(normalizedText)
                .rating(request.getRating())
                .build());

        String authorName = userRepository.findById(currentUserId).map(User::getName).orElse(null);
        notifyCommentParticipants(event, currentUserId, authorName, normalizedText);

        List<EventParticipant> participants = eventParticipantRepository.findByEventId(eventId);
        for (EventParticipant p : participants) {
            if (!p.getUserId().equals(currentUserId)) {
                try {
                    webSocketNotificationService.send(p.getUserId(), InAppNotificationDTO.builder()
                            .type(InAppNotificationType.EVENT_COMMENT)
                            .title("New comment on: " + event.getTitle())
                            .body(normalizedText)
                            .entityId(eventId)
                            .entityType("EVENT")
                            .build());
                } catch (Exception ex) {
                    log.warn("Failed to send WebSocket notification for event comment {} to user {}", saved.getId(), p.getUserId(), ex);
                }
            }
        }

        return EventCommentDTO.builder()
                .id(saved.getId())
                .authorUserId(saved.getAuthorUserId())
                .authorName(authorName)
                .text(saved.getText())
                .rating(saved.getRating())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    @PreAuthorize("@securityUtils.isCurrentUser(@eventCommentRepository.findById(#commentId).orElse(null)?.authorUserId) "
            + "or @organizationPermissionChecker.hasOrganizationPermission(#organizationId, 'EVENT_WRITE_COMMENT')")
    public void deleteEventComment(Long organizationId, Long eventId, Long commentId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        EventComment comment = eventCommentRepository
                .findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));

        if (!comment.getEventId().equals(eventId)) {
            throw new BusinessException("Comment " + commentId + " does not belong to event " + eventId);
        }

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        eventCommentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public EventCommentsByEventPageDTO getEventCommentsByEventIds(
            Long organizationId, List<Long> eventIds, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);
        validateCalendarPageable(pageable);

        if (eventIds == null || eventIds.isEmpty()) {
            throw new IllegalArgumentException("eventIds must not be empty");
        }

        List<Long> normalizedEventIds = eventIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (normalizedEventIds.isEmpty()) {
            throw new IllegalArgumentException("eventIds must contain at least one positive id");
        }

        int fromIndex = pageable.getPageNumber() * pageable.getPageSize();
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), normalizedEventIds.size());
        List<Long> pageEventIds = fromIndex >= normalizedEventIds.size()
                ? List.of()
                : normalizedEventIds.subList(fromIndex, toIndex);

        List<EventCommentsByEventDTO> content = pageEventIds.isEmpty()
                ? List.of()
                : buildEventCommentsGroups(organizationId, pageEventIds);

        int totalPages = (int) Math.ceil((double) normalizedEventIds.size() / pageable.getPageSize());
        return EventCommentsByEventPageDTO.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(normalizedEventIds.size())
                .totalPages(totalPages)
                .first(pageable.getPageNumber() == 0)
                .last(pageable.getPageNumber() >= Math.max(totalPages - 1, 0))
                .content(content)
                .build();
    }

    @Transactional(readOnly = true)
    public EventDTO getEventForCurrentUser(Long organizationId, Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        validateEventOrganization(event, organizationId);

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        return buildEventDto(event);
    }

    @Transactional(readOnly = true)
    public List<EventDTO> getEventsForCurrentUserInOrganization(Long organizationId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        List<Event> events = eventRepository.findByOrganizationId(organizationId);
        if (events.isEmpty()) {
            return List.of();
        }

        return buildEventDtoList(events);
    }

    @Transactional(readOnly = true)
    public EventCalendarGroupedResponseDTO getCalendarForCurrentUserInOrganization(
            Long organizationId, EventCalendarRequestDTO request, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);
        validateCalendarPageable(pageable);

        EventCalendarRequestDTO calendarRequest = request != null ? request : new EventCalendarRequestDTO();
        EventCalendarScope scope = calendarRequest.scopeAsEnum();

        CalendarRange range = resolveCalendarRange(calendarRequest.getFrom(), calendarRequest.getTo());
        boolean includeOrgWide = calendarRequest.getIncludeOrgWide() == null
                || calendarRequest.getIncludeOrgWide();

        Specification<Event> spec = Specification.where(EventSpecifications.organizationEquals(organizationId))
                .and(EventSpecifications.intersectsDateRange(range.from(), range.to()))
                .and(EventSpecifications.titleContains(calendarRequest.normalizedTitle()))
                .and(EventSpecifications.hasAnyTag(calendarRequest.normalizedTags()));

        switch (scope) {
            case ORGANIZATION -> {
                // All accepted organization members can access organization scope.
            }
            case SECTION -> {
                Long sectionId = calendarRequest.getSectionId();
                validateSectionBelongsToOrganization(organizationId, sectionId);
                ensureSectionCalendarAccess(organizationId, sectionId);
                spec = spec.and(buildSectionScopeSpec(List.of(sectionId), includeOrgWide));
            }
            case SECTIONS -> {
                List<Long> sectionIds = calendarRequest.normalizedSectionIds();
                validateSectionsBelongToOrganization(organizationId, sectionIds);
                for (Long sectionId : sectionIds) {
                    ensureSectionCalendarAccess(organizationId, sectionId);
                }
                spec = spec.and(buildSectionScopeSpec(sectionIds, includeOrgWide));
            }
        }

        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);
        return toGroupedCalendarResponse(eventsPage, pageable, scope, calendarRequest, includeOrgWide);
    }

    @Transactional(readOnly = true)
    public EventCalendarGroupedResponseDTO getCurrentUserCalendarInOrganization(
            Long organizationId, EventUserCalendarRequestDTO request, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);
        validateCalendarPageable(pageable);

        EventUserCalendarRequestDTO calendarRequest = request != null ? request : new EventUserCalendarRequestDTO();
        CalendarRange range = resolveCalendarRange(calendarRequest.getFrom(), calendarRequest.getTo());
        Specification<Event> spec = Specification.where(EventSpecifications.organizationEquals(organizationId))
                .and(EventSpecifications.intersectsDateRange(range.from(), range.to()))
                .and(EventSpecifications.hasParticipantUser(currentUserId))
                .and(EventSpecifications.titleContains(calendarRequest.normalizedTitle()))
                .and(EventSpecifications.hasAnyTag(calendarRequest.normalizedTags()));

        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);
        return toGroupedCalendarResponse(
                eventsPage,
                pageable,
                EventCalendarScope.ORGANIZATION,
                new EventCalendarRequestDTO(),
                true);
    }

    @Transactional(readOnly = true)
    public List<String> getOrganizationEventTags(Long organizationId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);
        return eventRepository.findDistinctTagsByOrganizationId(organizationId);
    }

    /**
     * Export the current user's schedule in iCalendar (ICS) format.
     *
     * Only upcoming events (based on their end time) in which the user
     * is a participant are included. The {@code includeDeclined} flag
     * controls whether events that the user has declined should be
     * included in the export.
     *
     * @param includeDeclined if {@code true}, events with RSVP status
     *                        DECLINED are also included; if {@code false},
     *                        they are excluded.
     * @return ICS file contents as a text string
     */
    @Transactional(readOnly = true)
    public String exportCurrentUserScheduleAsIcal(boolean includeDeclined) {
        Long currentUserId = securityUtils.getCurrentUserId();

        List<EventParticipant> participants = eventParticipantRepository.findByUserId(currentUserId);
        if (participants.isEmpty()) {
            return buildEmptyIcsCalendar();
        }

        // Собираем события, в которых пользователь участвует
        Set<Long> eventIds = participants.stream()
                .map(EventParticipant::getEventId)
                .collect(Collectors.toSet());

        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.isEmpty()) {
            return buildEmptyIcsCalendar();
        }

        Map<Long, Event> eventsById = events.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .collect(Collectors.toMap(Event::getId, e -> e));

        Instant now = Instant.now();

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Orkestro//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");

        for (EventParticipant participant : participants) {
            Event event = eventsById.get(participant.getEventId());
            if (event == null) {
                continue;
            }

            // Только предстоящие/текущие события
            if (event.getEndTime() != null && !event.getEndTime().isAfter(now)) {
                continue;
            }

            // Исключаем отказавшиеся, если флаг не установлен
            if (!includeDeclined && participant.getRsvpStatus() == EventRsvpStatus.DECLINED) {
                continue;
            }

            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:")
                    .append("event-")
                    .append(event.getId())
                    .append("@orkestro\r\n");
            sb.append("DTSTAMP:")
                    .append(formatInstantForIcs(now))
                    .append("\r\n");
            if (event.getStartTime() != null) {
                sb.append("DTSTART:")
                        .append(formatInstantForIcs(event.getStartTime()))
                        .append("\r\n");
            }
            if (event.getEndTime() != null) {
                sb.append("DTEND:")
                        .append(formatInstantForIcs(event.getEndTime()))
                        .append("\r\n");
            }
            if (event.getTitle() != null) {
                sb.append("SUMMARY:")
                        .append(escapeIcsText(event.getTitle()))
                        .append("\r\n");
            }
            if (event.getDescription() != null) {
                sb.append("DESCRIPTION:")
                        .append(escapeIcsText(event.getDescription()))
                        .append("\r\n");
            }
            if (event.getLocation() != null) {
                sb.append("LOCATION:")
                        .append(escapeIcsText(event.getLocation()))
                        .append("\r\n");
            }
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    /**
     * Export the current user's schedule as CSV.
     *
     * Only upcoming events (based on their end time) in which the user
     * is a participant are included. The {@code includeDeclined} flag
     * controls whether events that the user has declined should be
     * included in the export.
     *
     * Columns:
     * title,organization_id,start_time_utc,end_time_utc,location,rsvp_status
     *
     * @param includeDeclined if {@code true}, events with RSVP status
     *                        DECLINED are also included; if {@code false},
     *                        they are excluded.
     * @return CSV contents as a text string (including header row)
     */
    @Transactional(readOnly = true)
    public String exportCurrentUserScheduleAsCsv(boolean includeDeclined) {
        Long currentUserId = securityUtils.getCurrentUserId();

        List<EventParticipant> participants = eventParticipantRepository.findByUserId(currentUserId);
        String header = "title,organization_id,start_time_utc,end_time_utc,location,rsvp_status\n";
        if (participants.isEmpty()) {
            return header;
        }

        Set<Long> eventIds = participants.stream()
                .map(EventParticipant::getEventId)
                .collect(Collectors.toSet());

        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.isEmpty()) {
            return header;
        }

        Map<Long, Event> eventsById = events.stream()
                .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
                .collect(Collectors.toMap(Event::getId, e -> e));

        Instant now = Instant.now();

        StringBuilder sb = new StringBuilder();
        sb.append(header);

        for (EventParticipant participant : participants) {
            Event event = eventsById.get(participant.getEventId());
            if (event == null) {
                continue;
            }

            // Only upcoming/current events
            if (event.getEndTime() != null && !event.getEndTime().isAfter(now)) {
                continue;
            }

            // Exclude declined if flag is false
            if (!includeDeclined && participant.getRsvpStatus() == EventRsvpStatus.DECLINED) {
                continue;
            }

            sb.append(escapeCsv(event.getTitle()))
                    .append(',');
            sb.append(event.getOrganizationId() != null ? event.getOrganizationId() : "")
                    .append(',');
            sb.append(event.getStartTime() != null ? event.getStartTime().toString() : "")
                    .append(',');
            sb.append(event.getEndTime() != null ? event.getEndTime().toString() : "")
                    .append(',');
            sb.append(escapeCsv(event.getLocation()))
                    .append(',');
            sb.append(participant.getRsvpStatus() != null ? participant.getRsvpStatus().name() : "")
                    .append('\n');
        }

        return sb.toString();
    }

    private void ensureUserInOrganization(Long organizationId, Long userId) {
        organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + organizationId));
    }

    private void notifyCommentParticipants(Event event, Long authorUserId, String authorName, String commentText) {
        try {
            List<Long> recipientUserIds = eventParticipantRepository.findByEventId(event.getId()).stream()
                    .map(EventParticipant::getUserId)
                    .filter(userId -> !userId.equals(authorUserId))
                    .distinct()
                    .toList();
            if (recipientUserIds.isEmpty()) {
                return;
            }
            eventNotificationService.sendEventCommentNotifications(event, recipientUserIds, authorName, commentText);
        } catch (Exception ex) {
            log.error(
                    "Failed to notify participants about comment for event {} by user {}",
                    event.getId(),
                    authorUserId,
                    ex);
        }
    }

    private List<EventCommentsByEventDTO> buildEventCommentsGroups(Long organizationId, List<Long> eventIds) {
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            throw new EntityNotFoundException("One or more events not found for ids: " + eventIds);
        }
        for (Event event : events) {
            validateEventOrganization(event, organizationId);
        }

        List<EventComment> comments = eventCommentRepository.findByEventIdInOrderByCreatedAtDesc(eventIds);
        Set<Long> authorUserIds = comments.stream()
                .map(EventComment::getAuthorUserId)
                .collect(Collectors.toSet());
        Map<Long, String> userNamesById = userRepository.findAllById(authorUserIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        Map<Long, List<EventCommentDTO>> commentsByEventId = comments.stream()
                .collect(Collectors.groupingBy(
                        EventComment::getEventId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                c -> EventCommentDTO.builder()
                                        .id(c.getId())
                                        .authorUserId(c.getAuthorUserId())
                                        .authorName(userNamesById.get(c.getAuthorUserId()))
                                        .text(c.getText())
                                        .rating(c.getRating())
                                        .createdAt(c.getCreatedAt())
                                        .build(),
                                Collectors.toList())));

        return eventIds.stream()
                .map(eventId -> {
                    List<EventCommentDTO> eventComments = commentsByEventId.getOrDefault(eventId, List.of());
                    return EventCommentsByEventDTO.builder()
                            .eventId(eventId)
                            .commentsCount(eventComments.size())
                            .comments(eventComments)
                            .build();
                })
                .toList();
    }

    private void validateEventOrganization(Event event, Long organizationId) {
        if (!organizationId.equals(event.getOrganizationId())) {
            throw new BusinessException(
                    "Event " + event.getId() + " does not belong to organization " + organizationId);
        }
    }

    private Map<Long, EventParticipantSourceType> buildParticipantSources(
            Long organizationId,
            List<Long> participantUserIds,
            List<Long> participantSectionIds,
            boolean includeAllOrganizationMembers) {

        Map<Long, EventParticipantSourceType> userSources = new HashMap<>();

        // 1. Все участники организации (ORGANIZATION)
        if (includeAllOrganizationMembers) {
            List<OrganizationUser> orgUsers = organizationUserRepository.findByOrganizationIdAndStatus(
                    organizationId, OrganizationUserStatusType.ACCEPTED);
            for (OrganizationUser ou : orgUsers) {
                mergeSource(userSources, ou.getUserId(), EventParticipantSourceType.ORGANIZATION);
            }
        }

        // 2. Участники из секций (SECTION)
        if (participantSectionIds != null && !participantSectionIds.isEmpty()) {
            Set<Long> uniqueSectionIds = new HashSet<>(participantSectionIds);

            List<Section> sections = sectionRepository.findAllById(uniqueSectionIds);
            if (sections.size() != uniqueSectionIds.size()) {
                Set<Long> existingIds = sections.stream().map(Section::getId).collect(Collectors.toSet());
                Set<Long> missing = uniqueSectionIds.stream()
                        .filter(id -> !existingIds.contains(id))
                        .collect(Collectors.toSet());
                throw new EntityNotFoundException("One or more sections not found: " + missing);
            }

            for (Section section : sections) {
                if (!organizationId.equals(section.getOrganizationId())) {
                    throw new BusinessException(
                            "Section " + section.getId() + " does not belong to organization " + organizationId);
                }
            }

            List<SectionUser> sectionUsers = sectionUserRepository.findBySectionIdIn(uniqueSectionIds);
            for (SectionUser su : sectionUsers) {
                mergeSource(userSources, su.getUserId(), EventParticipantSourceType.SECTION);
            }
        }

        // 3. Явно указанные пользователи (MANUAL)
        if (participantUserIds != null && !participantUserIds.isEmpty()) {
            for (Long userId : participantUserIds) {
                if (userId != null) {
                    mergeSource(userSources, userId, EventParticipantSourceType.MANUAL);
                }
            }
        }

        if (userSources.isEmpty()) {
            return Map.of();
        }

        Set<Long> uniqueUserIds = new HashSet<>(userSources.keySet());

        // Проверяем существование пользователей
        List<Long> existingUserIds = userRepository.findAllById(uniqueUserIds).stream().map(u -> u.getId()).toList();
        if (existingUserIds.size() != uniqueUserIds.size()) {
            Set<Long> existingSet = new HashSet<>(existingUserIds);
            Set<Long> missing = uniqueUserIds.stream()
                    .filter(id -> !existingSet.contains(id))
                    .collect(Collectors.toSet());
            throw new EntityNotFoundException("One or more users not found: " + missing);
        }

        // Проверяем, что все пользователи входят в организацию
        List<OrganizationUser> organizationUsers = organizationUserRepository.findByOrganizationIdAndStatus(
                organizationId, OrganizationUserStatusType.ACCEPTED);
        Map<Long, Boolean> membership = organizationUsers.stream()
                .collect(Collectors.toMap(
                        OrganizationUser::getUserId,
                        ou -> Boolean.TRUE,
                        (a, b) -> a));

        List<Long> notMembers = uniqueUserIds.stream()
                .filter(id -> !membership.containsKey(id))
                .toList();
        if (!notMembers.isEmpty()) {
            throw new BusinessException(
                    "Users are not accepted members of organization " + organizationId + ": " + notMembers);
        }

        return userSources;
    }

    private void saveParticipants(Long eventId, Map<Long, EventParticipantSourceType> userSources) {
        if (userSources == null || userSources.isEmpty()) {
            return;
        }

        List<EventParticipant> entities = new ArrayList<>();
        for (Map.Entry<Long, EventParticipantSourceType> entry : userSources.entrySet()) {
            Long userId = entry.getKey();
            EventParticipantSourceType source = entry.getValue();

            EventParticipant participant = EventParticipant.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .source(source)
                    .rsvpStatus(EventRsvpStatus.PENDING)
                    .attendanceStatus(EventAttendanceStatus.UNKNOWN)
                    .rsvpAt(null)
                    .build();
            entities.add(participant);
        }

        eventParticipantRepository.saveAll(entities);
    }

    private List<Long> normalizeAndValidateParticipantSectionIds(
            Long organizationId, List<Long> participantSectionIds, boolean includeAllOrganizationMembers) {
        if (includeAllOrganizationMembers) {
            return List.of();
        }
        if (participantSectionIds == null || participantSectionIds.isEmpty()) {
            return List.of();
        }

        Set<Long> uniqueSectionIds = participantSectionIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
        if (uniqueSectionIds.isEmpty()) {
            return List.of();
        }

        List<Section> sections = sectionRepository.findAllById(uniqueSectionIds);
        if (sections.size() != uniqueSectionIds.size()) {
            Set<Long> existingIds = sections.stream().map(Section::getId).collect(Collectors.toSet());
            Set<Long> missing = uniqueSectionIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toSet());
            throw new EntityNotFoundException("One or more sections not found: " + missing);
        }

        for (Section section : sections) {
            if (!organizationId.equals(section.getOrganizationId())) {
                throw new BusinessException(
                        "Section " + section.getId() + " does not belong to organization " + organizationId);
            }
        }

        return sections.stream().map(Section::getId).toList();
    }

    private void saveEventSections(Long eventId, List<Long> sectionIds, boolean includeAllOrganizationMembers) {
        if (includeAllOrganizationMembers || sectionIds == null || sectionIds.isEmpty()) {
            return;
        }

        List<EventSection> entities = sectionIds.stream()
                .map(sectionId -> {
                    EventSection eventSection = new EventSection();
                    eventSection.setEventId(eventId);
                    eventSection.setSectionId(sectionId);
                    return eventSection;
                })
                .toList();

        eventSectionRepository.saveAll(entities);
    }

    private void saveEventFiles(Long eventId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        if (new HashSet<>(fileIds).size() > fileLimitsProperties.getEventMaxFiles()) {
            throw new BusinessException("Event files limit reached (" + fileLimitsProperties.getEventMaxFiles() + ")");
        }

        List<EventFile> entities = fileIds.stream()
                .map(fileId -> {
                    EventFile ef = new EventFile();
                    ef.setEventId(eventId);
                    ef.setFileId(fileId);
                    return ef;
                })
                .toList();

        eventFileRepository.saveAll(entities);
    }

    private List<Long> uploadEventFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > fileLimitsProperties.getEventMaxFiles()) {
            throw new IllegalArgumentException("Event files limit is " + fileLimitsProperties.getEventMaxFiles());
        }

        List<Long> uploadedFileIds = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.isEmpty() || file.getSize() <= 0) {
                throw new IllegalArgumentException("files[" + i + "] file is required");
            }
            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                throw new IllegalArgumentException("files[" + i + "] file name is required");
            }
            StoredFile stored = fileStorageService.uploadForCurrentUser(file, FileTypeDetector.detect(file));
            uploadedFileIds.add(stored.getId());
        }
        return uploadedFileIds;
    }

    private void validateSongsForOrganization(Long organizationId, List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>(songIds);
        List<Song> songs = songRepository.findAllById(uniqueIds);
        if (songs.size() != uniqueIds.size()) {
            throw new EntityNotFoundException("One or more songs not found for ids: " + uniqueIds);
        }

        for (Song song : songs) {
            if (!organizationId.equals(song.getOrganizationId())) {
                throw new BusinessException(
                        "Song " + song.getId() + " does not belong to organization " + organizationId);
            }
        }
    }

    private void saveEventSongs(Long eventId, List<Long> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            return;
        }

        List<EventSong> entities = new ArrayList<>();
        for (int i = 0; i < songIds.size(); i++) {
            Long songId = songIds.get(i);
            if (songId == null) {
                continue;
            }
            EventSong es = new EventSong();
            es.setEventId(eventId);
            es.setSongId(songId);
            es.setPosition(i);
            entities.add(es);
        }

        if (!entities.isEmpty()) {
            eventSongRepository.saveAll(entities);
        }
    }

    private String buildEmptyIcsCalendar() {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Orkestro//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private String formatInstantForIcs(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC);
        return formatter.format(instant);
    }

    private String escapeIcsText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
        return escaped;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private void mergeSource(
            Map<Long, EventParticipantSourceType> userSources, Long userId, EventParticipantSourceType newSource) {
        EventParticipantSourceType existing = userSources.get(userId);
        if (existing == null || sourcePriority(newSource) > sourcePriority(existing)) {
            userSources.put(userId, newSource);
        }
    }

    private int sourcePriority(EventParticipantSourceType source) {
        return switch (source) {
            case ORGANIZATION -> 1;
            case SECTION -> 2;
            case MANUAL -> 3;
        };
    }

    private void validateCalendarPageable(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable must not be null");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageable.getPageSize() <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        if (pageable.getPageSize() > CALENDAR_MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be <= " + CALENDAR_MAX_PAGE_SIZE);
        }
    }

    private CalendarRange resolveCalendarRange(Instant from, Instant to) {
        Instant now = Instant.now();
        Instant resolvedFrom = from != null ? from : now.minus(CALENDAR_DEFAULT_PAST_WINDOW);
        Instant resolvedTo = to != null ? to : now.plus(CALENDAR_DEFAULT_FUTURE_WINDOW);

        if (!resolvedTo.isAfter(resolvedFrom)) {
            throw new IllegalArgumentException("to must be after from");
        }
        if (Duration.between(resolvedFrom, resolvedTo).compareTo(CALENDAR_MAX_WINDOW) > 0) {
            throw new IllegalArgumentException(
                    "Date window must be <= " + CALENDAR_MAX_WINDOW.toDays() + " days");
        }
        return new CalendarRange(resolvedFrom, resolvedTo);
    }

    private void validateSectionBelongsToOrganization(Long organizationId, Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Section not found: " + sectionId));
        if (!organizationId.equals(section.getOrganizationId())) {
            throw new BusinessException("Section " + sectionId + " does not belong to organization " + organizationId);
        }
    }

    private void validateSectionsBelongToOrganization(Long organizationId, List<Long> sectionIds) {
        List<Section> sections = sectionRepository.findAllById(sectionIds);
        if (sections.size() != sectionIds.size()) {
            Set<Long> existingIds = sections.stream().map(Section::getId).collect(Collectors.toSet());
            List<Long> missing = sectionIds.stream().filter(id -> !existingIds.contains(id)).toList();
            throw new EntityNotFoundException("One or more sections not found: " + missing);
        }
        for (Section section : sections) {
            if (!organizationId.equals(section.getOrganizationId())) {
                throw new BusinessException(
                        "Section " + section.getId() + " does not belong to organization " + organizationId);
            }
        }
    }

    private void ensureSectionCalendarAccess(Long organizationId, Long sectionId) {
        boolean allowed = organizationPermissionChecker.isSectionMember(sectionId)
                || organizationPermissionChecker.hasSectionPermission(sectionId, "EVENT_VIEW_SECTION_CALENDAR")
                || organizationPermissionChecker.hasOrganizationPermission(organizationId,
                        "EVENT_VIEW_SECTION_CALENDAR");
        if (!allowed) {
            throw new BusinessException("Access denied to section calendar for section " + sectionId);
        }
    }

    private Specification<Event> buildSectionScopeSpec(List<Long> sectionIds, boolean includeOrgWide) {
        Specification<Event> scopeSpec = EventSpecifications.hasAnySection(sectionIds);
        if (includeOrgWide) {
            scopeSpec = scopeSpec.or(EventSpecifications.includeAllOrganizationMembers());
        }
        return scopeSpec;
    }

    private EventCalendarGroupedResponseDTO toGroupedCalendarResponse(
            Page<Event> eventsPage,
            Pageable pageable,
            EventCalendarScope scope,
            EventCalendarRequestDTO request,
            boolean includeOrgWide) {
        List<Event> events = eventsPage.getContent();
        if (events.isEmpty()) {
            return EventCalendarGroupedResponseDTO.builder()
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .totalElements(eventsPage.getTotalElements())
                    .totalPages(eventsPage.getTotalPages())
                    .first(eventsPage.isFirst())
                    .last(eventsPage.isLast())
                    .sectionGroups(List.of())
                    .organizationWideEvents(List.of())
                    .build();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<EventSection> eventSections = eventSectionRepository.findByEventIdIn(eventIds);

        Map<Long, List<Long>> eventSectionsMap = eventSections.stream()
                .collect(Collectors.groupingBy(
                        EventSection::getEventId,
                        Collectors.mapping(EventSection::getSectionId, Collectors.toList())));

        List<Long> groupingSectionIds = resolveGroupingSectionIds(scope, request, eventSectionsMap);
        Map<Long, List<EventCalendarDTO>> grouped = new LinkedHashMap<>();
        for (Long sectionId : groupingSectionIds) {
            grouped.put(sectionId, new ArrayList<>());
        }

        List<EventCalendarDTO> organizationWideEvents = new ArrayList<>();
        for (Event event : events) {
            EventCalendarDTO dto = toCalendarDto(event);
            if (event.isIncludeAllOrganizationMembers() && includeOrgWide) {
                organizationWideEvents.add(dto);
            }

            List<Long> eventSectionIds = eventSectionsMap.getOrDefault(event.getId(), List.of());
            if (scope == EventCalendarScope.SECTION || scope == EventCalendarScope.SECTIONS) {
                for (Long sectionId : groupingSectionIds) {
                    if (eventSectionIds.contains(sectionId)) {
                        grouped.computeIfAbsent(sectionId, ignored -> new ArrayList<>()).add(dto);
                    }
                }
            } else {
                for (Long sectionId : eventSectionIds) {
                    grouped.computeIfAbsent(sectionId, ignored -> new ArrayList<>()).add(dto);
                }
            }
        }

        List<EventCalendarSectionGroupDTO> sectionGroups = grouped.entrySet().stream()
                .map(entry -> EventCalendarSectionGroupDTO.builder()
                        .sectionId(entry.getKey())
                        .events(entry.getValue())
                        .build())
                .toList();

        return EventCalendarGroupedResponseDTO.builder()
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(eventsPage.getTotalElements())
                .totalPages(eventsPage.getTotalPages())
                .first(eventsPage.isFirst())
                .last(eventsPage.isLast())
                .sectionGroups(sectionGroups)
                .organizationWideEvents(organizationWideEvents)
                .build();
    }

    private List<Long> resolveGroupingSectionIds(
            EventCalendarScope scope,
            EventCalendarRequestDTO request,
            Map<Long, List<Long>> eventSectionsMap) {
        if (scope == EventCalendarScope.SECTION) {
            return List.of(request.getSectionId());
        }
        if (scope == EventCalendarScope.SECTIONS) {
            return request.normalizedSectionIds();
        }
        return eventSectionsMap.values().stream()
                .flatMap(List::stream)
                .distinct()
                .sorted()
                .toList();
    }

    private EventCalendarDTO toCalendarDto(Event event) {
        return EventCalendarDTO.builder()
                .id(event.getId())
                .organizationId(event.getOrganizationId())
                .title(event.getTitle())
                .eventType(event.getEventType())
                .location(event.getLocation())
                .tags(event.getTags() != null ? new ArrayList<>(event.getTags()) : List.of())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .build();
    }

    private Set<String> sanitizeTags(List<String> rawTags) {
        return rawTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private record CalendarRange(Instant from, Instant to) {
    }

    private EventDTO buildEventDto(Event event) {
        EventDTO dto = eventMapper.toDto(event);

        List<EventParticipant> participants = eventParticipantRepository.findByEventId(event.getId());
        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getUserId)
                .toList();

        dto.setParticipantUserIds(participantIds);
        dto.setIncludeAllOrganizationMembers(event.isIncludeAllOrganizationMembers());
        List<Long> participantSectionIds = eventSectionRepository.findByEventId(event.getId()).stream()
                .map(EventSection::getSectionId)
                .toList();
        dto.setParticipantSectionIds(participantSectionIds);
        List<EventFile> files = eventFileRepository.findByEventId(event.getId());
        List<Long> fileIds = files.stream().map(EventFile::getFileId).toList();
        dto.setFileIds(fileIds);
        List<EventSong> songs = eventSongRepository.findByEventId(event.getId());
        List<Long> songIds = songs.stream()
                .sorted((a, b) -> {
                    Integer pa = a.getPosition();
                    Integer pb = b.getPosition();
                    if (pa == null && pb == null) {
                        return 0;
                    }
                    if (pa == null) {
                        return 1;
                    }
                    if (pb == null) {
                        return -1;
                    }
                    return pa.compareTo(pb);
                })
                .map(EventSong::getSongId)
                .toList();
        dto.setSongIds(songIds);
        if (event.getTags() != null) {
            dto.setTags(new ArrayList<>(event.getTags()));
        } else {
            dto.setTags(List.of());
        }

        return dto;
    }

    private List<EventDTO> buildEventDtoList(List<Event> events) {
        List<EventDTO> dtos = eventMapper.toDtoList(events);
        if (dtos.isEmpty()) {
            return dtos;
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdIn(eventIds);
        List<EventSection> eventSections = eventSectionRepository.findByEventIdIn(eventIds);
        List<EventFile> eventFiles = eventFileRepository.findByEventIdIn(eventIds);
        List<EventSong> eventSongs = eventSongRepository.findByEventIdIn(eventIds);

        Map<Long, List<Long>> eventParticipantsMap = participants.stream()
                .collect(Collectors.groupingBy(
                        EventParticipant::getEventId,
                        Collectors.mapping(EventParticipant::getUserId, Collectors.toList())));

        Map<Long, List<Long>> eventSectionsMap = eventSections.stream()
                .collect(Collectors.groupingBy(
                        EventSection::getEventId,
                        Collectors.mapping(EventSection::getSectionId, Collectors.toList())));

        Map<Long, List<Long>> eventFilesMap = eventFiles.stream()
                .collect(Collectors.groupingBy(
                        EventFile::getEventId,
                        Collectors.mapping(EventFile::getFileId, Collectors.toList())));

        Map<Long, List<EventSong>> eventSongsMap = eventSongs.stream()
                .collect(Collectors.groupingBy(EventSong::getEventId));

        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            EventDTO dto = dtos.get(i);

            List<Long> participantIds = eventParticipantsMap.getOrDefault(event.getId(), List.of());
            dto.setParticipantUserIds(participantIds);
            dto.setIncludeAllOrganizationMembers(event.isIncludeAllOrganizationMembers());
            List<Long> participantSectionIds = eventSectionsMap.getOrDefault(event.getId(), List.of());
            dto.setParticipantSectionIds(participantSectionIds);

            List<Long> fileIds = eventFilesMap.getOrDefault(event.getId(), List.of());
            dto.setFileIds(fileIds);

            List<EventSong> songsForEvent = eventSongsMap.getOrDefault(event.getId(), List.of());
            List<Long> songIds = songsForEvent.stream()
                    .sorted((a, b) -> {
                        Integer pa = a.getPosition();
                        Integer pb = b.getPosition();
                        if (pa == null && pb == null) {
                            return 0;
                        }
                        if (pa == null) {
                            return 1;
                        }
                        if (pb == null) {
                            return -1;
                        }
                        return pa.compareTo(pb);
                    })
                    .map(EventSong::getSongId)
                    .toList();
            dto.setSongIds(songIds);

            if (event.getTags() != null) {
                dto.setTags(new ArrayList<>(event.getTags()));
            } else {
                dto.setTags(List.of());
            }
        }

        return dtos;
    }
}
