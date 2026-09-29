package com.example.customrag.search;

import java.util.List;

public record SearchOutcome(String answer, List<SourcePassage> sources) {}