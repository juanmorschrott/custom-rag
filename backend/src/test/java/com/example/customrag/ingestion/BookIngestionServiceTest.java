package com.example.customrag.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookIngestionServiceTest {

    @Test
    void removesSpecialTokenizerMarkersBeforeEmbedding() {
        assertEquals("Prompt: explain the answer", BookIngestionService.sanitizeForEmbedding(
                "Prompt: <|im_start|>explain the answer<|im_end|>"));
    }
}