package io.github.Romariok.orkestro.utils.helper;

import io.github.Romariok.orkestro.utils.file.FileStorageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public final class FileRollbackHelper {

    private final FileStorageService fileStorageService;

    public void deleteFilesSafely(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (Long fileId : fileIds) {
            if (fileId == null) {
                continue;
            }
            try {
                fileStorageService.delete(fileId);
            } catch (RuntimeException cleanupEx) {
                log.warn("Failed to rollback uploaded file {}", fileId, cleanupEx);
            }
        }
    }
}
