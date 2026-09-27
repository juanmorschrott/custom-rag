package com.example.customrag.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 300)
    private String author;

    @Column(name = "source_filename", nullable = false, length = 500)
    private String sourceFilename;

    @Column(nullable = false, unique = true, length = 64)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookStatus status = BookStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Book() {
    }

    public Book(String title, String author, String sourceFilename, String checksum) {
        this.title = title;
        this.author = author;
        this.sourceFilename = sourceFilename;
        this.checksum = checksum;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSourceFilename() {
        return sourceFilename;
    }

    public String getChecksum() {
        return checksum;
    }

    public BookStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markProcessing() {
        status = BookStatus.PROCESSING;
    }

    public void markIndexed() {
        status = BookStatus.INDEXED;
    }

    public void markExtracted() {
        status = BookStatus.EXTRACTED;
    }

    public void markFailed() {
        status = BookStatus.FAILED;
    }
}
