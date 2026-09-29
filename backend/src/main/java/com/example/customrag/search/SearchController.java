package com.example.customrag.search;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
            SearchOutcome outcome = searchService.search(request.query());
            return new SearchResponse(request.query(),
                            outcome.answer(),
                            outcome.sources().stream()
                                            .map(source -> new SearchResult(source.bookTitle(),
                                                            source.chapter(),
                                                            source.pageStart(),
                                                            source.pageEnd(),
                                                            source.excerpt(),
                                                            source.relevance()))
                                            .toList());
    }
}