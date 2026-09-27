package com.example.customrag.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ingestion/jobs")
@RequiredArgsConstructor
public class IngestionJobController {

    private final IngestionJobService ingestionJobService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IngestionJobView startScan() {
        return ingestionJobService.startScan();
    }

    @GetMapping("/{jobId}")
    public IngestionJobView getJob(@PathVariable UUID jobId) {
        return ingestionJobService.getJob(jobId);
    }
}