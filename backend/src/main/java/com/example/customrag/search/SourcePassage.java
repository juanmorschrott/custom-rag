package com.example.customrag.search;

public record SourcePassage(
        String bookTitle,
        String chapter,
        int pageStart,
        int pageEnd,
        String excerpt,
        Double relevance) {
}