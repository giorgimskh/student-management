package com.example.student_management.export;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Template Method: the fixed recipe for turning records into a CSV file. The
 * steps are always the same ({@link #export()} is final): prepare, header row,
 * then one row per record, each cell escaped. Subclasses fill in only what
 * differs: the file name, the columns, the records and how one record becomes
 * a row.
 *
 * <p>Output follows RFC 4180 (CRLF line endings, quotes doubled inside quoted
 * cells). Cells that a spreadsheet would run as a formula ({@code = + - @}) are
 * prefixed with an apostrophe, so a student named {@code =HYPERLINK(...)}
 * cannot run code when the file is opened in Excel.
 *
 * @param <T> the type of record exported
 */
public abstract class CsvExporter<T> {
    private static final String NEWLINE = "\r\n";

    /**
     * The template method. Final so every export follows the same steps.
     * Synchronized because exporters are singletons and {@link #prepare()} may
     * keep data in fields until the rows are written.
     */
    public final synchronized String export() {
        prepare();
        StringBuilder csv = new StringBuilder();
        appendRow(csv, new ArrayList<>(headers()));
        for (T record : records()) {
            appendRow(csv, toRow(record));
        }
        return csv.toString();
    }

    /** Key used in the URL, e.g. "students" for {@code /api/export/students.csv}. */
    public abstract String type();

    /** Column names, in order. */
    protected abstract List<String> headers();

    /** The records to export, in the order they should appear. */
    protected abstract List<T> records();

    /** One cell per column, in the same order as {@link #headers()}. Null becomes an empty cell. */
    protected abstract List<?> toRow(T record);

    /** Hook: runs before anything is written, e.g. to load lookup data. Does nothing by default. */
    protected void prepare() {
    }

    /** The download file name. */
    public String fileName() {
        return type() + ".csv";
    }

    private void appendRow(StringBuilder csv, List<?> cells) {
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
