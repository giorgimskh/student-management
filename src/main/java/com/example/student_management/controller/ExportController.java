package com.example.student_management.controller;

import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.export.CsvExporter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** CSV downloads: {@code GET /api/export/students.csv}, {@code books.csv}, {@code loans.csv}. */
@RestController
public class ExportController {
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final Map<String, CsvExporter<?>> exporters;

    /** Every {@link CsvExporter} bean is available by its {@link CsvExporter#type()}. */
    public ExportController(List<CsvExporter<?>> exporters) {
        this.exporters = exporters.stream()
                .collect(Collectors.toMap(CsvExporter::type, Function.identity(), (a, b) -> {
                    throw new IllegalStateException("Two CSV exporters share the type '" + a.type() + "'");
                }, TreeMap::new));
    }

    @GetMapping("/api/export/{type}.csv")
    public ResponseEntity<String> export(@PathVariable String type) {
        CsvExporter<?> exporter = exporters.get(type);
        if (exporter == null) {
            throw new ResourceNotFoundException("No CSV export '" + type + "'. Available: " + String.join(", ", exporters.keySet()));
        }
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(exporter.fileName()).build().toString())
                .body(exporter.export());
    }
}
