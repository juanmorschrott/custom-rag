package com.example.customrag.catalog;

import java.time.Instant;
import java.util.UUID;

public record BookSummary(UUID id, String title, String sourceFilename, BookStatus status, Instant createdAt) {}