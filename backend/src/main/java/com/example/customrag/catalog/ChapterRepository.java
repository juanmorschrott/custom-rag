package com.example.customrag.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    List<Chapter> findAllByBook_IdOrderByChapterNumberAsc(UUID bookId);

    void deleteAllByBook_Id(UUID bookId);
}
