package com.example.student_management.export;

import org.springframework.http.MediaType;

import java.util.List;

/**
 * Abstract Factory: one file format (CSV, JSON, ...) is one family of writers.
 * A family's writers must be used together: a CSV table inside a JSON report,
 * or a file named {@code .json} that holds CSV, would be broken output. Callers
 * pick a family once and get every writer, the media type and the file
 * extension from it, so they cannot mix families by mistake.
 *
 * <p>Adding a format means adding one implementation; the exporters and
 * controllers do not change.
 */
public interface ExportFormatFactory {
    /** File extension and URL key, e.g. "csv" for {@code /api/export/students.csv}. */
    String extension();

    /** Content type of everything this family writes. */
    MediaType mediaType();

    /** A writer for a list of records with fixed columns, e.g. every student. */
    TableWriter createTableWriter(List<String> headers);

    /** A writer for a report made of single values plus named tables, e.g. the statistics. */
    ReportWriter createReportWriter(String title);
}
