package com.example.customrag.ingestion;

public enum IngestionJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    FAILED
}