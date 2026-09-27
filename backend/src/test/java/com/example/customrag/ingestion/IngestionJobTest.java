package com.example.customrag.ingestion;

import com.example.customrag.catalog.BookStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IngestionJobTest {

    @Test
    void tracksPerBookResultsAndCompletesWithErrorsWhenNeeded() {
        IngestionJob job = new IngestionJob();
        job.start(3);
        job.setCurrentFilename("indexed.pdf");
        job.record(new BookScanResult("indexed.pdf", BookStatus.INDEXED, 4, 5, "Indexed"));
        job.setCurrentFilename("duplicate.pdf");
        job.record(new BookScanResult("duplicate.pdf", BookStatus.INDEXED, 0, 0, "Already registered: checksum matches"));
        job.setCurrentFilename("failed.pdf");
        job.record(new BookScanResult("failed.pdf", BookStatus.FAILED, 0, 0, "No extractable text"));
        job.complete();

        assertEquals(IngestionJobStatus.COMPLETED_WITH_ERRORS, job.getStatus());
        assertEquals(3, job.getProcessedFiles());
        assertEquals(1, job.getIndexedFiles());
        assertEquals(1, job.getSkippedFiles());
        assertEquals(1, job.getFailedFiles());
        assertEquals("failed.pdf: No extractable text", job.getLastError());
        assertNull(job.getCurrentFilename());
    }

    @Test
    void completesImmediatelyWhenLibraryIsEmpty() {
        IngestionJob job = new IngestionJob();

        job.start(0);
        job.complete();

        assertEquals(IngestionJobStatus.COMPLETED, job.getStatus());
        assertEquals(0, job.getTotalFiles());
        assertEquals(0, job.getProcessedFiles());
    }
}
