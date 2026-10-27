package com.example.customrag.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.customrag.catalog.Book;
import com.example.customrag.catalog.BookChunk;
import com.example.customrag.catalog.BookChunkRepository;
import com.example.customrag.catalog.BookRepository;
import com.example.customrag.catalog.BookStatus;
import com.example.customrag.catalog.Chapter;
import com.example.customrag.catalog.ChapterRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookIngestionService {

    private static final String FILE_EXTENSION = ".pdf";
    private static final String ALREADY_REGISTERED_MESSAGE = "Already registered: checksum matches an existing book";
    
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
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(FILE_EXTENSION))
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
        log.info("Starting book scan: {} PDF file(s) selected from '{}'", pdfFiles.size(), booksDirectory);

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
            .filter(result -> ALREADY_REGISTERED_MESSAGE.equals(result.message()))
            .count();
        
        int failed = (int) results.stream()
            .filter(result -> result.status() == BookStatus.FAILED)
            .count();

        log.info("Book scan completed: discovered={}, indexed={}, skipped={}, failed={}",
            pdfFiles.size(), ingested, skipped, failed);
        return new ScanReport(pdfFiles.size(), ingested, skipped, failed, results);
    }

    static String sanitizeForEmbedding(String text) {
        return text.replace("\u0000", "").replaceAll("<\\|[^\\s|]+\\|>", " ")
                .replaceAll("\\s+", " ").strip();
    }

    private BookScanResult ingest(Path pdfPath) {
        String filename = pdfPath.getFileName().toString();
        IngestionContext context = new IngestionContext();
        try {
            String checksum = checksum(pdfPath);
            Optional<Book> existingBook = bookRepository.findByChecksum(checksum);

            if (existingBook.filter(book -> book.getStatus() == BookStatus.INDEXED).isPresent()) {
                log.debug("Skipping already indexed PDF '{}'", filename);
                return new BookScanResult(filename, BookStatus.INDEXED, 0, 0, ALREADY_REGISTERED_MESSAGE);
            }

            context.book = existingBook.map(this::resetBook)
                    .orElseGet(() -> bookRepository.save(new Book(titleOf(filename), null, filename, checksum)));
            context.book.markProcessing();
            bookRepository.save(context.book);

            return indexBook(pdfPath, filename, context);
        } catch (Exception exception) {
            return failIngestion(filename, context, exception);
        }
    }

    private BookScanResult indexBook(Path pdfPath, String filename, IngestionContext context) throws IOException {
        Book book = context.book;
        List<PageText> pages = pdfTextExtractor.extract(pdfPath);
        context.pageCount = pages.size();

        List<TextChunk> chunks = textChunker.chunk(pages);
        if (chunks.isEmpty()) {
            log.warn("No extractable text found in PDF '{}'; OCR may be required", filename);
            book.markFailed();
            bookRepository.save(book);
            return new BookScanResult(filename, BookStatus.FAILED, context.pageCount, 0,
                    "No extractable text found; scanned PDFs need OCR");
        }

        List<Chapter> chapters = saveChapters(book, chapterDetector.detect(pdfPath));
        List<BookChunk> savedChunks = bookChunkRepository.saveAll(toBookChunks(book, chapters, chunks));
        vectorStore.add(savedChunks.stream().map(BookIngestionService::toVectorDocument).toList());

        book.markIndexed();
        bookRepository.save(book);
        return new BookScanResult(filename, BookStatus.INDEXED, context.pageCount, chunks.size(),
                "Text and embeddings indexed with page references");
    }

    private BookScanResult failIngestion(String filename, IngestionContext context, Exception exception) {
        if (context.book != null) {
            context.book.markFailed();
            bookRepository.save(context.book);
        }
        log.error("Failed to ingest PDF '{}'", filename, exception);
        String message = Objects.requireNonNullElse(exception.getMessage(), "PDF processing failed");
        return new BookScanResult(filename, BookStatus.FAILED, context.pageCount, 0, message);
    }

    private Book resetBook(Book book) {
        List<BookChunk> previousChunks = bookChunkRepository.findAllByBook_IdOrderByOrdinalAsc(book.getId());
        if (!previousChunks.isEmpty()) {
            vectorStore.delete(previousChunks.stream().map(chunk -> chunk.getId().toString()).toList());
        }
        bookChunkRepository.deleteAllByBook_Id(book.getId());
        chapterRepository.deleteAllByBook_Id(book.getId());
        return book;
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

    private static String titleOf(String filename) {
        return filename.substring(0, filename.length() - FILE_EXTENSION.length()).strip();
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

    private static final class IngestionContext {
        private Book book;
        private int pageCount;
    }
}
