package com.example.customrag.search;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SearchResultMapper {

    SearchResult toResult(SourcePassage passage);

    List<SearchResult> toResults(List<SourcePassage> passages);

    @Mapping(target = "answer", source = "outcome.answer")
    @Mapping(target = "results", source = "outcome.sources")
    SearchResponse toResponse(String query, SearchOutcome outcome);
}
