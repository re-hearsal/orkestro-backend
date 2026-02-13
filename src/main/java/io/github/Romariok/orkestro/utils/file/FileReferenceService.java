package io.github.Romariok.orkestro.utils.file;

import io.github.Romariok.orkestro.event.repository.EventFileRepository;
import io.github.Romariok.orkestro.organization.repository.OrganizationRepository;
import io.github.Romariok.orkestro.repertoire.repository.SongFileRepository;
import io.github.Romariok.orkestro.task.repository.TaskFileRepository;
import io.github.Romariok.orkestro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileReferenceService {

    private final EventFileRepository eventFileRepository;
    private final TaskFileRepository taskFileRepository;
    private final SongFileRepository songFileRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isFileReferenced(Long fileId) {
        return eventFileRepository.existsByFileId(fileId)
                || taskFileRepository.existsByFileId(fileId)
                || songFileRepository.existsByFileId(fileId)
                || organizationRepository.existsByProfileImageFileId(fileId)
                || userRepository.existsByProfileImageFileId(fileId);
    }
}
