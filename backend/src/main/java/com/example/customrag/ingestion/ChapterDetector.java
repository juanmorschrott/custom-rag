package com.example.customrag.ingestion;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class ChapterDetector {

    public List<ChapterBoundary> detect(Path pdfPath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            int pageCount = document.getNumberOfPages();
            Map<Integer, String> bookmarksByPage = new TreeMap<>();
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            
            if (outline != null) {
                collectBookmarks(document, pdfPath, outline, "", bookmarksByPage);
            }

            if (bookmarksByPage.isEmpty()) {
                return List.of(new ChapterBoundary(1, "Unclassified", 1, pageCount));
            }

            List<Map.Entry<Integer, String>> bookmarks = new ArrayList<>(bookmarksByPage.entrySet());
            List<ChapterBoundary> chapters = new ArrayList<>(bookmarks.size() + 1);
            
            if (bookmarks.getFirst().getKey() > 1) {
                chapters.add(new ChapterBoundary(1, "Front matter", 1, bookmarks.getFirst().getKey() - 1));
            }
            
            for (int index = 0; index < bookmarks.size(); index++) {
                Map.Entry<Integer, String> bookmark = bookmarks.get(index);
                int pageEnd = index + 1 < bookmarks.size()
                        ? bookmarks.get(index + 1).getKey() - 1
                        : pageCount;
                chapters.add(new ChapterBoundary(chapters.size() + 1, bookmark.getValue(), bookmark.getKey(), pageEnd));
            }
            
            return List.copyOf(chapters);
        }
    }

    private void collectBookmarks(
            PDDocument document,
            Path pdfPath,
            PDOutlineNode parent,
            String parentTitle,
            Map<Integer, String> bookmarksByPage) throws IOException {

        for (PDOutlineItem item : parent.children()) {
            String title = sanitizeTitle(item.getTitle());
            String fullTitle = parentTitle.isBlank() ? title : parentTitle + " / " + title;
            PDPage destinationPage = null;
            
            try {
                destinationPage = item.findDestinationPage(document);
            } catch (IOException | RuntimeException exception) {
                log.warn("Ignoring malformed bookmark '{}' in PDF '{}'; continuing with remaining bookmarks",
                        title, pdfPath, exception);
            }
            
            if (destinationPage != null && !title.isBlank()) {
                int pageNumber = document.getPages().indexOf(destinationPage) + 1;
                if (pageNumber > 0) {
                    bookmarksByPage.put(pageNumber, fullTitle);
                }
            }
            
            collectBookmarks(document, pdfPath, item, fullTitle, bookmarksByPage);
        }
    }

    static String sanitizeTitle(String title) {
        return title == null ? "" : title.replace("\u0000", "").strip();
    }
}
