package com.example.customrag.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookChunkRepository extends JpaRepository<BookChunk, UUID> {

    List<BookChunk> findAllByBook_IdOrderByOrdinalAsc(UUID bookId);

    void deleteAllByBook_Id(UUID bookId);
}