package com.example.customrag.search;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(@NotBlank String query) {}