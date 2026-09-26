package com.example.customrag.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChapterDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void derivesChapterPageRangesFromPdfBookmarks() throws IOException {
        Path pdfPath = tempDir.resolve("book.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage firstPage = new PDPage();
            PDPage secondPage = new PDPage();
            PDPage thirdPage = new PDPage();
            PDPage fourthPage = new PDPage();
            document.addPage(firstPage);
            document.addPage(secondPage);
            document.addPage(thirdPage);
            document.addPage(fourthPage);

            PDDocumentOutline outline = new PDDocumentOutline();
            PDOutlineItem firstChapter = new PDOutlineItem();
            firstChapter.setTitle("Foundations");
            firstChapter.setDestination(secondPage);
            outline.addLast(firstChapter);
            PDOutlineItem secondChapter = new PDOutlineItem();
            secondChapter.setTitle("Design");
            secondChapter.setDestination(thirdPage);
            outline.addLast(secondChapter);
            document.getDocumentCatalog().setDocumentOutline(outline);
            document.save(pdfPath.toFile());
        }

        List<ChapterBoundary> chapters = new ChapterDetector().detect(pdfPath);

        assertEquals(List.of(
            new ChapterBoundary(1, "Front matter", 1, 1),
            new ChapterBoundary(2, "Foundations", 2, 2),
            new ChapterBoundary(3, "Design", 3, 4)), chapters);
    }

    @Test
    void createsUnclassifiedChapterWhenPdfHasNoBookmarks() throws IOException {
        Path pdfPath = tempDir.resolve("no-bookmarks.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.addPage(new PDPage());
            document.save(pdfPath.toFile());
        }

        assertEquals(List.of(new ChapterBoundary(1, "Unclassified", 1, 2)),
                new ChapterDetector().detect(pdfPath));
    }

    @Test
    void removesNullBytesFromBookmarkTitles() {
        assertEquals("Chapter title", ChapterDetector.sanitizeTitle("Chapter\u0000 title"));
    }
}
