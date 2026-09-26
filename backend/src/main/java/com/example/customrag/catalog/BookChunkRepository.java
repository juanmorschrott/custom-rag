package com.example.customrag.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface BookChunkRepository extends JpaRepository<BookChunk, UUID> {

    List<BookChunk> findAllByBook_IdOrderByOrdinalAsc(UUID bookId);

    @Transactional
    void deleteAllByBook_Id(UUID bookId);
}