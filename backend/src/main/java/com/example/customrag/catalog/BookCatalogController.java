package com.example.customrag.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookCatalogController {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    @GetMapping
    public List<BookSummary> listBooks() {
        return bookMapper.toSummaries(bookRepository.findAll(Sort.by(Sort.Direction.ASC, "title")));
    }

}