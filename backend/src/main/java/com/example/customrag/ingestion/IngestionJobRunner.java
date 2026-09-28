package com.example.customrag.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionJobRunner {

    private final BookIngestionService bookIngestionService;
    private final IngestionJobRepository jobRepository;

    @Async
    public void run(UUID jobId) {

        IngestionJob job = jobRepository.findById(jobId).orElse(null);
        
        if (job == null) {
            log.warn("Cannot run ingestion job {}; it was not found", jobId);
            return;
        }

        try {
            int totalFiles = bookIngestionService.discoverPdfFiles().size();
            job.start(totalFiles);
            jobRepository.saveAndFlush(job);
            log.info("Running ingestion job {} for {} PDF file(s)", jobId, totalFiles);

            bookIngestionService.scanBooks(null,
                    filename -> {
                        job.setCurrentFilename(filename);
                        jobRepository.saveAndFlush(job);
                    },
                    result -> {
                        job.record(result);
                        jobRepository.saveAndFlush(job);
                    });

            job.complete();
            jobRepository.save(job);
            log.info("Completed ingestion job {}", jobId);
        } catch (Exception exception) {
            log.error("Ingestion job {} failed", jobId, exception);
            String message = exception.getMessage() == null ? "Ingestion job failed" : exception.getMessage();
            job.fail(message);
            jobRepository.save(job);
        }
    }
}