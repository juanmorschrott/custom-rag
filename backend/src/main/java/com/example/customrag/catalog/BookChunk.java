package com.example.customrag.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "book_chunks", uniqueConstraints = @UniqueConstraint(
        name = "uq_book_chunks_book_ordinal", columnNames = {"book_id", "ordinal"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "page_start", nullable = false)
    private int pageStart;

    @Column(name = "page_end", nullable = false)
    private int pageEnd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public BookChunk(Book book, Chapter chapter, int ordinal, int pageStart, int pageEnd, String content) {
        this.book = book;
        this.chapter = chapter;
        this.ordinal = ordinal;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.content = content;
    }
}