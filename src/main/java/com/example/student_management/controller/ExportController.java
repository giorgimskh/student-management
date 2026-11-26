package com.example.student_management.controller;

import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.export.ExportFormatFactory;
import com.example.student_management.export.ExportFormats;
import com.example.student_management.export.TableExporter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Downloads: {@code GET /api/export/{type}.{format}}, e.g. {@code students.csv}
 * or {@code loans.json}. Any exporter works with any format.
 */
@RestController
public class ExportController {
    private final Map<String, TableExporter<?>> exporters;
    private final ExportFormats formats;

    /** Every {@link TableExporter} bean is available by its {@link TableExporter#type()}. */
    public ExportController(List<TableExporter<?>> exporters, ExportFormats formats) {
        this.exporters = exporters.stream()
                .collect(Collectors.toMap(TableExporter::type, Function.identity(), (a, b) -> {
                    throw new IllegalStateException("Two exporters share the type '" + a.type() + "'");
                }, TreeMap::new));
        this.formats = formats;
    }

    @GetMapping("/api/export/{type:[^.]+}.{format:[^.]+}")
    public ResponseEntity<String> export(@PathVariable String type, @PathVariable String format) {
        TableExporter<?> exporter = exporters.get(type);
        if (exporter == null) {
            throw new ResourceNotFoundException("No export '" + type + "'. Available: " + String.join(", ", exporters.keySet()));
        }
        ExportFormatFactory factory = formats.get(format);
        return Downloads.attachment(exporter.fileName(factory), factory, exporter.export(factory));
    }
}
