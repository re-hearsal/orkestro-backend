package io.github.Romariok.orkestro.event.service;

import io.github.Romariok.orkestro.event.dto.EventCreateRequestDTO;
import io.github.Romariok.orkestro.event.dto.EventDTO;
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
import io.github.Romariok.orkestro.user.repository.UserRepository;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import io.github.Romariok.orkestro.utils.helper.FileValidationHelper;
import io.github.Romariok.orkestro.utils.exception.BusinessException;
import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

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
    private final StoredFileRepository storedFileRepository;
    private final EventNotificationService eventNotificationService;

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

        FileValidationHelper.validateFiles(request.getFileIds(), storedFileRepository);
        validateSongsForOrganization(organizationId, request.getSongIds());

        boolean includeAll = Boolean.TRUE.equals(request.getIncludeAllOrganizationMembers());
        Map<Long, EventParticipantSourceType> sources = buildParticipantSources(
                organizationId, request.getParticipantUserIds(), request.getParticipantSectionIds(), includeAll);

        Set<String> tags = request.getTags() != null ? sanitizeTags(request.getTags()) : null;

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
                .sendRsvp(false)
                .remindBeforeMinutes(null)
                .createdAt(now)
                .tags(tags)
                .build();

        Event saved = eventRepository.save(event);

        saveParticipants(saved.getId(), sources);
        saveEventFiles(saved.getId(), request.getFileIds());
        saveEventSongs(saved.getId(), request.getSongIds());

        eventNotificationService.sendEventCreatedNotifications(saved, sources.keySet());

        return buildEventDto(saved);
    }

    @Transactional
    public EventDTO updateEvent(Long eventId, EventUpdateRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

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

        if (request.getFileIds() != null) {
            FileValidationHelper.validateFiles(request.getFileIds(), storedFileRepository);
            eventFileRepository.deleteByEventId(eventId);
            saveEventFiles(eventId, request.getFileIds());
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
    @PreAuthorize("@securityUtils.isCurrentUser(@eventRepository.findById(#eventId).orElse(null)?.creatorUserId) "
            + "or hasAuthority('CTX_PERM_ORG:' + "
            + "@eventRepository.findById(#eventId).orElse(null)?.organizationId + ':EVENT_DELETION')")
    public void deleteEvent(Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        Long currentUserId = securityUtils.getCurrentUserId();
        ensureUserInOrganization(event.getOrganizationId(), currentUserId);

        eventParticipantRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }

    @Transactional(readOnly = true)
    public EventDTO getEventForCurrentUser(Long eventId) {
        Event event = eventRepository
                .findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

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

    private void ensureUserInOrganization(Long organizationId, Long userId) {
        organizationUserRepository
                .findByOrganizationIdAndUserId(organizationId, userId)
                .filter(ou -> ou.getStatus() == OrganizationUserStatusType.ACCEPTED)
                .orElseThrow(() -> new BusinessException(
                        "User " + userId + " is not an accepted member of organization " + organizationId));
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
