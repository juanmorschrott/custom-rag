package com.example.customrag.ingestion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

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