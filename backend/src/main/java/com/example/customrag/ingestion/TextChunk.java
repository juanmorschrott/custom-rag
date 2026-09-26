package com.example.customrag.ingestion;

public record TextChunk(int ordinal, int pageStart, int pageEnd, String text) {
}
