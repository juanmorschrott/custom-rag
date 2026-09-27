package com.example.customrag.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {

    Optional<IngestionJob> findFirstByStatusInOrderByCreatedAtDesc(Collection<IngestionJobStatus> statuses);
}