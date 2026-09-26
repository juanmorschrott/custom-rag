package com.example.customrag.ingestion;

import java.util.List;

public record ScanReport(int discovered, int ingested, int skipped, int failed, List<BookScanResult> books) {
}
