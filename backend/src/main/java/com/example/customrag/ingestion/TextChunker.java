package com.example.customrag.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final int maximumCharacters;
    private final int overlapCharacters;

    public TextChunker(
            @Value("${app.ingestion.chunk-size:4000}") int maximumCharacters,
            @Value("${app.ingestion.chunk-overlap:400}") int overlapCharacters) {
        if (maximumCharacters < 1 || overlapCharacters < 0 || overlapCharacters >= maximumCharacters) {
            throw new IllegalArgumentException("Chunk size must be positive and overlap smaller than chunk size");
        }
        this.maximumCharacters = maximumCharacters;
        this.overlapCharacters = overlapCharacters;
    }

    public List<TextChunk> chunk(List<PageText> pages) {
        List<TextChunk> chunks = new ArrayList<>();
        for (PageText page : pages) {
            String text = page.text().replaceAll("\\s+", " ").strip();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + maximumCharacters, text.length());
                if (end < text.length()) {
                    int boundary = text.lastIndexOf(' ', end);
                    if (boundary > start) {
                        end = boundary;
                    }
                }

                String chunkText = text.substring(start, end).strip();
                if (!chunkText.isEmpty()) {
                    chunks.add(new TextChunk(chunks.size(), page.pageNumber(), page.pageNumber(), chunkText));
                }
                if (end == text.length()) {
                    break;
                }
                start = Math.max(end - overlapCharacters, start + 1);
                while (start < text.length() && text.charAt(start) == ' ') {
                    start++;
                }
            }
        }
        return List.copyOf(chunks);
    }
}
