package com.example.student_management.export;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * The CSV family.
 *
 * <p>Output follows RFC 4180 (CRLF line endings, quotes doubled inside quoted
 * cells). Cells that a spreadsheet would run as a formula ({@code = + - @}) are
 * prefixed with an apostrophe, so a student named {@code =HYPERLINK(...)}
 * cannot run code when the file is opened in Excel.
 *
 * <p>A report is one file: a {@code name,value} line per field, then each
 * section after a blank line, as its name on a line of its own, its header row
 * and its rows.
 */
@Component
public class CsvFormatFactory implements ExportFormatFactory {
    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);
    private static final String NEWLINE = "\r\n";

    @Override
    public String extension() {
        return "csv";
    }

    @Override
    public MediaType mediaType() {
        return CSV;
    }

    @Override
    public TableWriter createTableWriter(List<String> headers) {
        return new CsvTableWriter(headers);
    }

    @Override
    public ReportWriter createReportWriter(String title) {
        return new CsvReportWriter(title);
    }

    private static final class CsvTableWriter implements TableWriter {
        private final StringBuilder csv = new StringBuilder();
        private final int columns;

        CsvTableWriter(List<String> headers) {
            columns = headers.size();
            appendRow(csv, headers);
        }

        @Override
        public void writeRow(List<?> cells) {
            Rows.requireColumns(cells, columns);
            appendRow(csv, cells);
        }

        @Override
        public String finish() {
            return csv.toString();
        }
    }

    private static final class CsvReportWriter implements ReportWriter {
        private final StringBuilder csv = new StringBuilder();

        CsvReportWriter(String title) {
            appendRow(csv, List.of("report", title));
        }

        @Override
        public ReportWriter field(String name, Object value) {
            appendRow(csv, Arrays.asList(name, value));
            return this;
        }

        @Override
        public ReportWriter section(String name, List<String> headers, List<? extends List<?>> rows) {
            csv.append(NEWLINE);
            appendRow(csv, List.of(name));
            appendRow(csv, headers);
            for (List<?> row : rows) {
                Rows.requireColumns(row, headers.size());
                appendRow(csv, row);
            }
            return this;
        }

        @Override
        public String finish() {
            return csv.toString();
        }
    }

    private static void appendRow(StringBuilder csv, List<?> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) csv.append(',');
            csv.append(escape(cells.get(i)));
        }
        csv.append(NEWLINE);
    }

    static String escape(Object value) {
        if (value == null) return "";
        String text = value instanceof Instant instant ? instant.toString() : String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 && !(value instanceof Number)) {
            text = "'" + text; // spreadsheet formula injection
        }
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}
