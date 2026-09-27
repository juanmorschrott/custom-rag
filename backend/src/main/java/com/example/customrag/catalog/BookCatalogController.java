package com.example.customrag.catalog;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookCatalogController {

    private final BookRepository bookRepository;

    @GetMapping
    public List<BookSummary> listBooks() {
        return bookRepository.findAll(Sort.by(Sort.Direction.ASC, "title")).stream()
                .map(book -> new BookSummary(book.getId(),
                        book.getTitle(),
                        book.getSourceFilename(),
                        book.getStatus(),
                        book.getCreatedAt()))
                .toList();
    }

    public record BookSummary(UUID id, String title, String sourceFilename, BookStatus status, Instant createdAt) {
    }
}