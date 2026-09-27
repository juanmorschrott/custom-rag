package com.example.customrag.ingestion;

import com.example.customrag.catalog.Book;
import com.example.customrag.catalog.BookChunk;
import com.example.customrag.catalog.BookChunkRepository;
import com.example.customrag.catalog.BookRepository;
import com.example.customrag.catalog.BookStatus;
import com.example.customrag.catalog.Chapter;
import com.example.customrag.catalog.ChapterRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Consumer;

@Service
public class BookIngestionService {

    private final Path booksDirectory;
    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;
    private final BookChunkRepository bookChunkRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ChapterDetector chapterDetector;
    private final TextChunker textChunker;
    private final VectorStore vectorStore;

    public BookIngestionService(
            @Value("${app.ingestion.books-directory:../data/books}") String booksDirectory,
            BookRepository bookRepository,
            ChapterRepository chapterRepository,
            BookChunkRepository bookChunkRepository,
            PdfTextExtractor pdfTextExtractor,
            ChapterDetector chapterDetector,
            TextChunker textChunker,
            VectorStore vectorStore) {
        this.booksDirectory = Path.of(booksDirectory).toAbsolutePath().normalize();
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
        this.bookChunkRepository = bookChunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.chapterDetector = chapterDetector;
        this.textChunker = textChunker;
        this.vectorStore = vectorStore;
    }

    public List<Path> discoverPdfFiles() throws IOException {
        Files.createDirectories(booksDirectory);
        try (var paths = Files.walk(booksDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .sorted()
                    .toList();
        }
    }

    public ScanReport scanBooks(String requestedFilename) throws IOException {
        return scanBooks(requestedFilename, ignored -> {}, ignored -> {});
    }

    public ScanReport scanBooks(
            String requestedFilename,
            Consumer<String> onFileStarted,
            Consumer<BookScanResult> onFileFinished) throws IOException {

        List<Path> pdfFiles = discoverPdfFiles().stream()
                .filter(path -> requestedFilename == null || path.getFileName().toString().equals(requestedFilename))
                .toList();

        List<BookScanResult> results = new ArrayList<>(pdfFiles.size());
        for (Path pdfFile : pdfFiles) {
            onFileStarted.accept(pdfFile.getFileName().toString());
            BookScanResult result = ingest(pdfFile);
            results.add(result);
            onFileFinished.accept(result);
        }

        int ingested = (int) results.stream()
            .filter(result -> result.status() == BookStatus.INDEXED)
            .count();
        
        int skipped = (int) results.stream()
            .filter(result -> result.message().startsWith("Already registered"))
            .count();
        
        int failed = (int) results.stream()
            .filter(result -> result.status() == BookStatus.FAILED)
            .count();
        
        return new ScanReport(pdfFiles.size(), ingested, skipped, failed, results);
    }

    private BookScanResult ingest(Path pdfPath) {
        String filename = pdfPath.getFileName().toString();
        Book book = null;
        int pageCount = 0;
        try {
            String checksum = checksum(pdfPath);
            var existingBook = bookRepository.findByChecksum(checksum);
            if (existingBook.isPresent() && existingBook.get().getStatus() == BookStatus.INDEXED) {
                return new BookScanResult(filename, existingBook.get().getStatus(), 0, 0,
                        "Already registered: checksum matches an existing book");
            }

            if (existingBook.isPresent()) {
                book = existingBook.get();
                List<BookChunk> previousChunks = bookChunkRepository.findAllByBook_IdOrderByOrdinalAsc(book.getId());
                if (!previousChunks.isEmpty()) {
                    vectorStore.delete(previousChunks.stream().map(chunk -> chunk.getId().toString()).toList());
                }
                bookChunkRepository.deleteAllByBook_Id(book.getId());
                chapterRepository.deleteAllByBook_Id(book.getId());
            } else {
                String title = filename.substring(0, filename.length() - 4).strip();
                book = bookRepository.save(new Book(title, null, filename, checksum));
            }
            book.markProcessing();
            bookRepository.save(book);

            List<PageText> pages = pdfTextExtractor.extract(pdfPath);
            pageCount = pages.size();
            List<TextChunk> chunks = textChunker.chunk(pages);
            if (chunks.isEmpty()) {
                book.markFailed();
                bookRepository.save(book);
                return new BookScanResult(filename, BookStatus.FAILED, pageCount, 0,
                        "No extractable text found; scanned PDFs need OCR");
            }

            List<ChapterBoundary> boundaries = chapterDetector.detect(pdfPath);
            Book ingestedBook = book;
            List<Chapter> chapters = chapterRepository.saveAll(boundaries.stream()
                    .map(boundary -> new Chapter(ingestedBook, boundary.chapterNumber(), boundary.title(),
                            boundary.pageStart(), boundary.pageEnd()))
                    .toList());

            List<BookChunk> bookChunks = new ArrayList<>(chunks.size());
            for (TextChunk chunk : chunks) {
                Chapter chapter = findChapter(chapters, chunk.pageStart());
                bookChunks.add(new BookChunk(book, chapter, chunk.ordinal(),
                        chunk.pageStart(), chunk.pageEnd(), chunk.text()));
            }
            List<BookChunk> savedChunks = bookChunkRepository.saveAll(bookChunks);
            vectorStore.add(savedChunks.stream().map(BookIngestionService::toVectorDocument).toList());

            book.markIndexed();
            bookRepository.save(book);
            return new BookScanResult(filename, BookStatus.INDEXED, pageCount, chunks.size(),
                    "Text and embeddings indexed with page references");
        } catch (Exception exception) {
            if (book != null) {
                book.markFailed();
                bookRepository.save(book);
            }
            String message = exception.getMessage() == null ? "PDF processing failed" : exception.getMessage();
            return new BookScanResult(filename, BookStatus.FAILED, pageCount, 0, message);
        }
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

    static String sanitizeForEmbedding(String text) {
        return text.replace("\u0000", "").replaceAll("<\\|[^\\s|]+\\|>", " ")
                .replaceAll("\\s+", " ").strip();
    }

    private Chapter findChapter(List<Chapter> chapters, int pageNumber) {
        return chapters.stream()
                .filter(chapter -> chapter.getPageStart() <= pageNumber && chapter.getPageEnd() >= pageNumber)
                .findFirst()
                .orElse(chapters.getFirst());
    }

    private String checksum(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
