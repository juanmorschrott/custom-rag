package com.example.customrag.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findAllByBook_IdOrderByChapterNumberAsc(UUID bookId);

    @Transactional
    void deleteAllByBook_Id(UUID bookId);
}
