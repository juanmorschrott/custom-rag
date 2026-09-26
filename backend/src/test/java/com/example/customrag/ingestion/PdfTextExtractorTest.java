package com.example.customrag.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsTextAndKeepsOneBasedPageNumbers() throws IOException {
        Path pdfPath = tempDir.resolve("two-pages.pdf");
        try (PDDocument document = new PDDocument()) {
            addTextPage(document, "First page text");
            addTextPage(document, "Second page text");
            document.save(pdfPath.toFile());
        }

        List<PageText> pages = new PdfTextExtractor().extract(pdfPath);

        assertEquals(2, pages.size());
        assertEquals(1, pages.get(0).pageNumber());
        assertTrue(pages.get(0).text().contains("First page text"));
        assertEquals(2, pages.get(1).pageNumber());
        assertTrue(pages.get(1).text().contains("Second page text"));
    }

    @Test
    void removesNullBytesBeforePersistingExtractedText() {
        assertEquals("text before and after", PdfTextExtractor.sanitize("text before\u0000 and after"));
    }

    private void addTextPage(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);
        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            content.newLineAtOffset(50, 700);
            content.showText(text);
            content.endText();
        }
    }
}
