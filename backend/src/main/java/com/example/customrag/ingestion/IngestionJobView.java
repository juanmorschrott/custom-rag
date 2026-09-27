package com.example.customrag.ingestion;

import java.time.Instant;
import java.util.UUID;

public record IngestionJobView(
        UUID id,
        IngestionJobStatus status,
        int totalFiles,
        int processedFiles,
        int indexedFiles,
        int skippedFiles,
        int failedFiles,
        String currentFilename,
        String lastError,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt) {
}