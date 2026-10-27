package com.example.customrag.ingestion;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.customrag.catalog.Book;
import com.example.customrag.catalog.BookChunk;
import com.example.customrag.catalog.BookChunkRepository;
import com.example.customrag.catalog.BookRepository;
import com.example.customrag.catalog.Chapter;
import com.example.customrag.catalog.ChapterRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persists a book's chapters, chunks and embeddings atomically: any failure rolls back
 * both the relational data and the vector store, which shares the same DataSource.
 */
@Component
@RequiredArgsConstructor
public class BookIndexer {

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final BookChunkRepository bookChunkRepository;
    private final VectorStore vectorStore;

    @Transactional
    public void replaceIndex(UUID bookId, List<ChapterBoundary> boundaries, List<TextChunk> chunks) {
        Book book = bookRepository.getReferenceById(bookId);

        deletePreviousIndex(bookId);

        List<Chapter> chapters = saveChapters(book, boundaries);
        List<BookChunk> savedChunks = bookChunkRepository.saveAll(toBookChunks(book, chapters, chunks));
        vectorStore.add(savedChunks.stream().map(BookIndexer::toVectorDocument).toList());

        book.markIndexed();
    }

    static String sanitizeForEmbedding(String text) {
        return text.replace("\u0000", "").replaceAll("<\\|[^\\s|]+\\|>", " ")
                .replaceAll("\\s+", " ").strip();
    }

    private void deletePreviousIndex(UUID bookId) {
        List<BookChunk> previousChunks = bookChunkRepository.findAllByBook_IdOrderByOrdinalAsc(bookId);
        if (!previousChunks.isEmpty()) {
            vectorStore.delete(previousChunks.stream().map(chunk -> chunk.getId().toString()).toList());
        }
        bookChunkRepository.deleteAllByBook_Id(bookId);
        chapterRepository.deleteAllByBook_Id(bookId);
    }

    private List<Chapter> saveChapters(Book book, List<ChapterBoundary> boundaries) {
        return chapterRepository.saveAll(boundaries.stream()
                .map(boundary -> new Chapter(book,
                        boundary.chapterNumber(),
                        boundary.title(),
                        boundary.pageStart(),
                        boundary.pageEnd()))
                .toList());
    }

    private List<BookChunk> toBookChunks(Book book, List<Chapter> chapters, List<TextChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new BookChunk(book, findChapter(chapters, chunk.pageStart()), chunk.ordinal(),
                        chunk.pageStart(), chunk.pageEnd(), chunk.text()))
                .toList();
    }

    private Chapter findChapter(List<Chapter> chapters, int pageNumber) {
        return chapters.stream()
                .filter(chapter -> chapter.getPageStart() <= pageNumber && chapter.getPageEnd() >= pageNumber)
                .findFirst()
                .orElse(chapters.getFirst());
    }

    private static Document toVectorDocument(BookChunk chunk) {
        return Document.builder()
                .id(chunk.getId().toString())
                .text(sanitizeForEmbedding(chunk.getContent()))
                .metadata("bookId", chunk.getBook().getId().toString())
                .metadata("bookTitle", chunk.getBook().getTitle())
                .metadata("chapter", chunk.getChapter().getTitle())
                .metadata("pageStart", chunk.getPageStart())
                .metadata("pageEnd", chunk.getPageEnd())
                .build();
    }
}
