package com.example.customrag.ingestion;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IngestionJobRunner {

    private final BookIngestionService bookIngestionService;
    private final IngestionJobRepository jobRepository;

    public IngestionJobRunner(
            BookIngestionService bookIngestionService,
            IngestionJobRepository jobRepository) {
        this.bookIngestionService = bookIngestionService;
        this.jobRepository = jobRepository;
    }

    @Async
    public void run(UUID jobId) {
        IngestionJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            int totalFiles = bookIngestionService.discoverPdfFiles().size();
            job.start(totalFiles);
            jobRepository.saveAndFlush(job);

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
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "Ingestion job failed" : exception.getMessage();
            job.fail(message);
            jobRepository.save(job);
        }
    }
}