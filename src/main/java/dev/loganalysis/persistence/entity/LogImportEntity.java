package dev.loganalysis.persistence.entity;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.importing.domain.ImportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "log_imports")
public class LogImportEntity {

    @Id private UUID id;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LogFormat format;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportStatus status;

    @Column(name = "storage_key", nullable = false, unique = true, length = 255)
    private String storageKey;

    @Column(name = "source_hint", length = 128)
    private String sourceHint;

    @Column(name = "service_hint", length = 128)
    private String serviceHint;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "accepted_lines", nullable = false)
    private int acceptedLines;

    @Column(name = "rejected_lines", nullable = false)
    private int rejectedLines;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Version
    @Column(nullable = false)
    private long version;

    protected LogImportEntity() {}

    private LogImportEntity(
            UUID id,
            String originalFilename,
            LogFormat format,
            String storageKey,
            String sourceHint,
            String serviceHint,
            Instant createdAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.format = format;
        this.status = ImportStatus.QUEUED;
        this.storageKey = storageKey;
        this.sourceHint = sourceHint;
        this.serviceHint = serviceHint;
        this.createdAt = createdAt;
    }

    public static LogImportEntity queued(
            String originalFilename,
            LogFormat format,
            String storageKey,
            String sourceHint,
            String serviceHint,
            Instant now) {
        return new LogImportEntity(
                UUID.randomUUID(),
                originalFilename,
                format,
                storageKey,
                sourceHint,
                serviceHint,
                now);
    }

    public void start(Instant now, Instant leaseUntil) {
        if (status != ImportStatus.QUEUED && status != ImportStatus.PROCESSING) {
            throw new IllegalStateException("only queued or expired processing imports can start");
        }
        status = ImportStatus.PROCESSING;
        startedAt = startedAt == null ? now : startedAt;
        this.leaseUntil = leaseUntil;
    }

    public void recordBatch(int total, int accepted, int rejected, Instant leaseUntil) {
        if (status != ImportStatus.PROCESSING) {
            throw new IllegalStateException("only processing imports can record progress");
        }
        totalLines += total;
        acceptedLines += accepted;
        rejectedLines += rejected;
        this.leaseUntil = leaseUntil;
    }

    public void complete(Instant now) {
        if (status != ImportStatus.PROCESSING) {
            throw new IllegalStateException("only processing imports can complete");
        }
        status = ImportStatus.COMPLETED;
        completedAt = now;
        leaseUntil = null;
    }

    public void fail(String code, Instant now) {
        if (status == ImportStatus.COMPLETED) {
            throw new IllegalStateException("completed imports cannot fail");
        }
        status = ImportStatus.FAILED;
        failureCode = code;
        completedAt = now;
        leaseUntil = null;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public LogFormat getFormat() {
        return format;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getSourceHint() {
        return sourceHint;
    }

    public String getServiceHint() {
        return serviceHint;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public int getAcceptedLines() {
        return acceptedLines;
    }

    public int getRejectedLines() {
        return rejectedLines;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }
}
