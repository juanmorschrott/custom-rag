package com.example.customrag.ingestion;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class BookIngestionController {

    private final BookIngestionService bookIngestionService;

    @PostMapping("/scan")
    public ScanReport scanBooks(@RequestParam(required = false) String filename) throws IOException {
        return bookIngestionService.scanBooks(filename);
    }
}
