package io.github.Romariok.orkestro.utils.helper;

import io.github.Romariok.orkestro.utils.exception.EntityNotFoundException;
import io.github.Romariok.orkestro.utils.file.StoredFile;
import io.github.Romariok.orkestro.utils.file.StoredFileRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Общий helper для валидации существования файлов по идентификаторам.
 */
public final class FileValidationHelper {

    private FileValidationHelper() {
    }

    /**
     * Проверяет, что все файлы с указанными идентификаторами существуют.
     * Если список пустой или null — ничего не делает.
     *
     * @throws EntityNotFoundException если хотя бы один файл не найден.
     */
    public static void validateFiles(List<Long> fileIds, StoredFileRepository storedFileRepository) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>(fileIds);
        List<StoredFile> files = storedFileRepository.findAllById(uniqueIds);
        if (files.size() != uniqueIds.size()) {
            throw new EntityNotFoundException("One or more files not found for ids: " + uniqueIds);
        }
    }
}
