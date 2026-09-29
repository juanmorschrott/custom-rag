package com.example.customrag.search;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Slf4j
@Service
public class SearchService {

    private static final String SYSTEM_INSTRUCTIONS = """
            You are a study assistant. Answer in the same language as the question and use only the supplied book passages.
            Cite each claim with its exact passage number, such as [1] or [2]. The source list maps those numbers to book, chapter, and page.
            If the passages do not support an answer, say that the indexed passages are insufficient.
            Never invent book titles, chapter names, quotations, or page numbers.
            """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public SearchService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public SearchOutcome search(String query) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.25)
                .build());

        List<SourcePassage> sources = documents.stream()
                .map(SearchService::toSourcePassage)
                .toList();
        
        if (documents.isEmpty()) {
            log.info("Search found no relevant passages");
            return new SearchOutcome("No he encontrado pasajes relevantes en los libros indexados.", sources);
        }

        String sourceContext = IntStream.range(0, documents.size())
                .mapToObj(index -> formatSource(index + 1, documents.get(index)))
                .reduce((first, second) -> first + "\n\n" + second)
                .orElse("");

        String answer = chatClient.prompt()
                .system(SYSTEM_INSTRUCTIONS)
                .user("Question: " + query + "\n\nBook passages:\n" + sourceContext)
                .call()
                .content();

        log.info("Search completed with {} source passage(s)", sources.size());
        return new SearchOutcome(answer, sources);
    }

    private static SourcePassage toSourcePassage(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new SourcePassage(
                metadataString(metadata, "bookTitle"),
                metadataString(metadata, "chapter"),
                metadataInteger(metadata, "pageStart"),
                metadataInteger(metadata, "pageEnd"),
                document.getText(),
                document.getScore());
    }

    private static String formatSource(int index, Document document) {
        Map<String, Object> metadata = document.getMetadata();

        String pages = metadataInteger(metadata, "pageStart") == metadataInteger(metadata, "pageEnd")
                ? "p. " + metadataInteger(metadata, "pageStart")
                : "pp. " + metadataInteger(metadata, "pageStart") + "-" + metadataInteger(metadata, "pageEnd");

        return "[" + index + "] " + metadataString(metadata, "bookTitle") + ", "
                + metadataString(metadata, "chapter") + ", " + pages + "\n"
                + document.getText();
    }

    private static String metadataString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? "Unknown" : value.toString();
    }

    private static int metadataInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
}