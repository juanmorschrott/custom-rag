package com.example.customrag.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionJobService {

    private static final List<IngestionJobStatus> ACTIVE_STATUSES =
            List.of(IngestionJobStatus.QUEUED, IngestionJobStatus.RUNNING);

    private final IngestionJobRepository jobRepository;
    private final IngestionJobRunner jobRunner;
    private final IngestionJobMapper jobMapper;

    public synchronized IngestionJobView startScan() {
        var activeJob = jobRepository.findFirstByStatusInOrderByCreatedAtDesc(ACTIVE_STATUSES);
        
        if (activeJob.isPresent()) {
            return jobMapper.toView(activeJob.get());
        }

        IngestionJob job = jobRepository.saveAndFlush(new IngestionJob());
        jobRunner.run(job.getId());
        
        return jobMapper.toView(job);
    }

    public IngestionJobView getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(jobMapper::toView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found"));
    }
}