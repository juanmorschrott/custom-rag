package com.example.customrag.ingestion;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class IngestionJobService {

    private static final List<IngestionJobStatus> ACTIVE_STATUSES =
            List.of(IngestionJobStatus.QUEUED, IngestionJobStatus.RUNNING);

    private final IngestionJobRepository jobRepository;
    private final IngestionJobRunner jobRunner;

    public IngestionJobService(
            IngestionJobRepository jobRepository,
            IngestionJobRunner jobRunner) {
        this.jobRepository = jobRepository;
        this.jobRunner = jobRunner;
    }

    public synchronized IngestionJobView startScan() {
        var activeJob = jobRepository.findFirstByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES);
        if (activeJob.isPresent()) {
            return IngestionJobView.from(activeJob.get());
        }

        IngestionJob job = jobRepository.saveAndFlush(new IngestionJob());
        jobRunner.run(job.getId());
        return IngestionJobView.from(job);
    }

    public IngestionJobView getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(IngestionJobView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found"));
    }
}