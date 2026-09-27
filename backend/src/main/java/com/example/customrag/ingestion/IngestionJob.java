package com.example.customrag.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingestion_jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IngestionJobStatus status = IngestionJobStatus.QUEUED;

    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    @Column(name = "processed_files", nullable = false)
    private int processedFiles;

    @Column(name = "indexed_files", nullable = false)
    private int indexedFiles;

    @Column(name = "skipped_files", nullable = false)
    private int skippedFiles;

    @Column(name = "failed_files", nullable = false)
    private int failedFiles;

    @Setter
    @Column(name = "current_filename", length = 500)
    private String currentFilename;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void start(int totalFiles) {
        this.totalFiles = totalFiles;
        this.startedAt = Instant.now();
        this.status = totalFiles == 0 ? IngestionJobStatus.COMPLETED : IngestionJobStatus.RUNNING;
        if (totalFiles == 0) {
            this.finishedAt = Instant.now();
        }
    }

    public void record(BookScanResult result) {
        processedFiles++;
        currentFilename = null;
        if (result.message().startsWith("Already registered")) {
            skippedFiles++;
        } else if (result.status() == com.example.customrag.catalog.BookStatus.INDEXED) {
            indexedFiles++;
        } else {
            failedFiles++;
            lastError = result.filename() + ": " + result.message();
        }
    }

    public void complete() {
        status = failedFiles == 0 ? IngestionJobStatus.COMPLETED : IngestionJobStatus.COMPLETED_WITH_ERRORS;
        currentFilename = null;
        finishedAt = Instant.now();
    }

    public void fail(String message) {
        status = IngestionJobStatus.FAILED;
        lastError = message;
        currentFilename = null;
        finishedAt = Instant.now();
    }
}