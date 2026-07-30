package dev.loganalysis.importing;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.persistence.entity.LogImportEntity;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LogImportService {

    private final LogImportStorage storage;
    private final LogImportRepository repository;
    private final Clock clock;

    public LogImportService(LogImportStorage storage, LogImportRepository repository, Clock clock) {
        this.storage = storage;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public LogImportEntity submit(
            MultipartFile file, LogFormat format, String sourceHint, String serviceHint) {
        String storageKey = storage.store(file);
        try {
            LogImportEntity logImport =
                    LogImportEntity.queued(
                            file.getOriginalFilename(),
                            format,
                            storageKey,
                            normalizeHint(sourceHint),
                            normalizeHint(serviceHint),
                            clock.instant());
            return repository.saveAndFlush(logImport);
        } catch (RuntimeException error) {
            storage.deleteQuietly(storageKey);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public LogImportEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new LogImportNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LogImportEntity> list() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private static String normalizeHint(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 128) {
            throw new InvalidImportException(
                    "invalid_import_hint",
                    "Source and service hints must not exceed 128 characters.");
        }
        return normalized;
    }
}
