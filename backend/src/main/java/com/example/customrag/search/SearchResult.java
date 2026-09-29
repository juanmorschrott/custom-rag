package com.example.customrag.search;

public record SearchResult(
        String bookTitle,
        String chapter,
        int pageStart,
        int pageEnd,
        String excerpt,
        Double relevance) {
}