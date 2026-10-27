package com.example.customrag.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookIndexerTest {

    @Test
    void removesSpecialTokenizerMarkersBeforeEmbedding() {
        assertEquals("Prompt: explain the answer", BookIndexer.sanitizeForEmbedding(
                "Prompt: <|im_start|>explain the answer<|im_end|>"));
    }
}
