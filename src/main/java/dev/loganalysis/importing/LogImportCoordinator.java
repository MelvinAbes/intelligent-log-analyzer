package dev.loganalysis.importing;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.importing.domain.ImportStatus;
import dev.loganalysis.persistence.entity.LogImportEntity;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogImportCoordinator {

    private final LogImportRepository repository;
    private final AnalysisProperties properties;
    private final Clock clock;

    public LogImportCoordinator(
            LogImportRepository repository, AnalysisProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Optional<UUID> claimNext() {
        Instant now = clock.instant();
        return repository
                .findNextClaimable(now)
                .map(
                        logImport -> {
                            logImport.start(now, now.plus(properties.imports().leaseDuration()));
                            return logImport.getId();
                        });
    }

    @Transactional(readOnly = true)
    public LogImportEntity get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new LogImportNotFoundException(id));
    }

    @Transactional
    public void complete(UUID id) {
        LogImportEntity logImport = getForUpdate(id);
        logImport.complete(clock.instant());
    }

    @Transactional
    public void fail(UUID id, String code) {
        LogImportEntity logImport = getForUpdate(id);
        if (logImport.getStatus() != ImportStatus.COMPLETED) {
            logImport.fail(code, clock.instant());
        }
    }

    private LogImportEntity getForUpdate(UUID id) {
        return repository.findById(id).orElseThrow(() -> new LogImportNotFoundException(id));
    }
}
