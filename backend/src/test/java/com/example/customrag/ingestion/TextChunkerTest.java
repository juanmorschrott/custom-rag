package com.example.customrag.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitsLongPageWithConfiguredOverlapAndPageMetadata() {
        TextChunker chunker = new TextChunker(10, 3);

        List<TextChunk> chunks = chunker.chunk(List.of(
                new PageText(4, "abcdefghijklmnopqrst")));

        assertEquals(3, chunks.size());
        assertEquals("abcdefghij", chunks.get(0).text());
        assertEquals("hijklmnopq", chunks.get(1).text());
        assertEquals("opqrst", chunks.get(2).text());
        assertEquals(List.of(0, 1, 2), chunks.stream().map(TextChunk::ordinal).toList());
        assertTrue(chunks.stream().allMatch(chunk -> chunk.pageStart() == 4 && chunk.pageEnd() == 4));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.text().length() <= 10));
    }

    @Test
    void skipsBlankPages() {
        TextChunker chunker = new TextChunker(20, 2);

        List<TextChunk> chunks = chunker.chunk(List.of(
                new PageText(1, "  \n "),
                new PageText(2, "Useful text")));

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.getFirst().pageStart());
        assertEquals("Useful text", chunks.getFirst().text());
    }
}
