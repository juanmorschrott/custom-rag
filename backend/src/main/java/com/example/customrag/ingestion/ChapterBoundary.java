package com.example.customrag.ingestion;

public record ChapterBoundary(int chapterNumber, String title, int pageStart, int pageEnd) {
}
