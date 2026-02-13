package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.dto.EventAttendanceRowDTO;
import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
import io.github.Romariok.orkestro.event.dto.EventSearchRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventUpdateRequestDTO;
import io.github.Romariok.orkestro.event.mapper.EventMapper;
import io.github.Romariok.orkestro.event.models.Event;
import io.github.Romariok.orkestro.event.models.EventFile;
import io.github.Romariok.orkestro.event.models.EventParticipant;
import io.github.Romariok.orkestro.event.models.EventSong;
import io.github.Romariok.orkestro.event.models.enums.EventAttendanceStatus;
import io.github.Romariok.orkestro.event.models.enums.EventParticipantSourceType;
import io.github.Romariok.orkestro.event.models.enums.EventRsvpStatus;
import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.event.repository.EventParticipantRepository;
import io.github.Romariok.orkestro.event.repository.EventRepository;
import io.github.Romariok.orkestro.event.repository.EventSongRepository;
import io.github.Romariok.orkestro.event.specification.EventSpecifications;
import io.github.Romariok.orkestro.repertoire.models.Song;
import io.github.Romariok.orkestro.repertoire.repository.SongRepository;
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
import io.github.Romariok.orkestro.utils.file.FileStorageService;
import io.github.Romariok.orkestro.utils.file.FileReferenceService;
import io.github.Romariok.orkestro.utils.file.FileTypeDetector;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.helper.FileRollbackHelper;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final int MAX_EVENT_FILES = 50;

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventFileRepository eventFileRepository;
    private final EventSongRepository eventSongRepository;
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

    @Transactional
    public EventDTO createEventInOrganization(Long organizationId, EventCreateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Event title must not be blank");
        }

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type must not be null");
        }

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Event start and end time must not be null");
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
        Map<Long, EventParticipantSourceType> sources = buildParticipantSources(
                organizationId, request.getParticipantUserIds(), request.getParticipantSectionIds(), includeAll);

        Set<String> tags = request.getTags() != null ? sanitizeTags(request.getTags()) : null;

        boolean sendRsvp = Boolean.TRUE.equals(request.getSendRsvp());
        Integer remindBeforeMinutes = request.getRemindBeforeMinutes();

        Event event = Event.builder()
                .organizationId(organizationId)
                .creatorUserId(currentUserId)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .externalLink(request.getExternalLink())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .sendRsvp(sendRsvp)
                .remindBeforeMinutes(remindBeforeMinutes)
                .createdAt(now)
                .tags(tags)
                .build();

        List<Long> uploadedFileIds = List.of();
        try {
            uploadedFileIds = uploadEventFiles(request.getFiles());

            Event saved = eventRepository.save(event);

            saveParticipants(saved.getId(), sources);
            saveEventFiles(saved.getId(), uploadedFileIds);
            saveEventSongs(saved.getId(), request.getSongIds());

            if (sendRsvp) {
                eventNotificationService.sendEventCreatedNotifications(saved, sources.keySet());
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
        boolean participantsChanged = request.getParticipantUserIds() != null
                || request.getParticipantSectionIds() != null
                || request.getIncludeAllOrganizationMembers() != null;
        if (participantsChanged) {
            boolean includeAll = Boolean.TRUE.equals(request.getIncludeAllOrganizationMembers());
            Map<Long, EventParticipantSourceType> sources = buildParticipantSources(
                    event.getOrganizationId(),
                    request.getParticipantUserIds(),
                    request.getParticipantSectionIds(),
                    includeAll);

            eventParticipantRepository.deleteByEventId(eventId);
            saveParticipants(eventId, sources);
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

        if (source.getStartTime() == null || source.getEndTime() == null || !source.getEndTime().isAfter(source.getStartTime())) {
            throw new IllegalStateException("Source event has invalid time range");
        }
        long durationSeconds = source.getEndTime().getEpochSecond() - source.getStartTime().getEpochSecond();

        List<EventParticipant> sourceParticipants = eventParticipantRepository.findByEventId(eventId);
        List<EventFile> sourceFiles = eventFileRepository.findByEventId(eventId);
        List<EventSong> sourceSongs = eventSongRepository.findByEventId(eventId);

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
        if (currentFiles.size() >= MAX_EVENT_FILES) {
            throw new BusinessException("Event files limit reached (" + MAX_EVENT_FILES + ")");
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

    /**
     * Поиск доступных пользователю мероприятий в организации по названию и тегам.
     * Если оба фильтра пустые/отсутствуют — возвращаются все события организации.
     */
    @Transactional(readOnly = true)
    public List<EventDTO> searchEventsForCurrentUserInOrganization(
            Long organizationId, String titleQuery, List<String> tagFilters) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        String normalizedTitle = titleQuery == null ? null : titleQuery.trim();
        if (normalizedTitle != null && normalizedTitle.isEmpty()) {
            normalizedTitle = null;
        }

        Set<String> normalizedTags = tagFilters == null ? Set.of() : sanitizeTags(tagFilters);

        List<Event> events = eventRepository.findByOrganizationId(organizationId);
        if (events.isEmpty()) {
            return List.of();
        }

        final String titleFilter = normalizedTitle;
        final Set<String> tagsFilter = normalizedTags;

        List<Event> filtered = events.stream()
                .filter(event -> {
                    if (titleFilter != null) {
                        String title = event.getTitle();
                        if (title == null
                                || !title.toLowerCase(Locale.ROOT)
                                        .contains(titleFilter.toLowerCase(Locale.ROOT))) {
                            return false;
                        }
                    }

                    if (!tagsFilter.isEmpty()) {
                        Set<String> eventTags = event.getTags();
                        if (eventTags == null || !eventTags.containsAll(tagsFilter)) {
                            return false;
                        }
                    }

                    return true;
                })
                .toList();

        if (filtered.isEmpty()) {
            return List.of();
        }

        return buildEventDtoList(filtered);
    }

    @Transactional(readOnly = true)
    public Page<EventDTO> searchEventsPageForCurrentUserInOrganization(
            Long organizationId, EventSearchRequestDTO request, Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(organizationId, currentUserId);

        String titleQuery = request != null ? request.getTitle() : null;
        List<String> tagFilters = request != null ? request.getTags() : null;
        Instant from = request != null ? request.getFrom() : null;
        Instant to = request != null ? request.getTo() : null;

        String normalizedTitle = titleQuery == null ? null : titleQuery.trim();
        if (normalizedTitle != null && normalizedTitle.isEmpty()) {
            normalizedTitle = null;
        }
        final String titleFilter = normalizedTitle;
        Set<String> normalizedTags = tagFilters == null ? Set.of() : sanitizeTags(tagFilters);

        Specification<Event> spec = Specification.where(EventSpecifications.organizationEquals(organizationId))
                .and(EventSpecifications.titleContainsIgnoreCase(titleFilter))
                .and(EventSpecifications.hasAllTags(normalizedTags))
                .and(EventSpecifications.intersectsDateRange(from, to));

        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);
        List<EventDTO> content = buildEventDtoList(eventsPage.getContent());
        return new PageImpl<>(content, pageable, eventsPage.getTotalElements());
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
            String uidBase = event.getTitle() != null ? event.getTitle() : String.valueOf(event.getId());
            sb.append("UID:")
                    .append(escapeIcsText(uidBase))
                    .append("-")
                    .append(currentUserId)
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

    private void saveEventFiles(Long eventId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        if (new HashSet<>(fileIds).size() > MAX_EVENT_FILES) {
            throw new BusinessException("Event files limit reached (" + MAX_EVENT_FILES + ")");
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
        if (files.size() > MAX_EVENT_FILES) {
            throw new IllegalArgumentException("Event files limit is " + MAX_EVENT_FILES);
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

    private Set<String> sanitizeTags(List<String> rawTags) {
        return rawTags.stream()
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .map(tag -> tag.trim())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private EventDTO buildEventDto(Event event) {
        EventDTO dto = eventMapper.toDto(event);

        List<EventParticipant> participants = eventParticipantRepository.findByEventId(event.getId());
        List<Long> participantIds = participants.stream()
                .map(EventParticipant::getUserId)
                .toList();

        dto.setParticipantUserIds(participantIds);
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
        List<EventFile> eventFiles = eventFileRepository.findByEventIdIn(eventIds);
        List<EventSong> eventSongs = eventSongRepository.findByEventIdIn(eventIds);

        Map<Long, List<Long>> eventParticipantsMap = participants.stream()
                .collect(Collectors.groupingBy(
                        EventParticipant::getEventId,
                        Collectors.mapping(EventParticipant::getUserId, Collectors.toList())));

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
