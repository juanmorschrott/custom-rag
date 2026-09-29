package com.example.customrag.search;

import java.util.List;

public record SearchResponse(String query, String answer, List<SearchResult> results) {}