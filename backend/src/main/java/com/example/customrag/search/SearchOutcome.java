package com.example.customrag.search;

import java.util.List;

public record SearchOutcome(String answer, List<SourcePassage> sources) {

    public record SourcePassage(
            String bookTitle,
            String chapter,
            int pageStart,
            int pageEnd,
            String excerpt,
            Double relevance) {
    }
}