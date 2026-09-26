package com.example.customrag.ingestion;

import com.example.customrag.catalog.BookStatus;

public record BookScanResult(
        String filename,
        BookStatus status,
        int pageCount,
        int chunkCount,
        String message) {
}
