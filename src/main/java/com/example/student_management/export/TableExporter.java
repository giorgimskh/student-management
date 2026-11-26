package com.example.student_management.export;

import java.util.List;

/**
 * Template Method: the fixed recipe for exporting records as a table. The steps
 * are always the same ({@link #export(ExportFormatFactory)} is final): prepare,
 * header row, then one row per record. Subclasses fill in only what differs:
 * the name, the columns, the records and how one record becomes a row.
 *
 * <p>The file format is not part of the recipe. The caller passes an
 * {@link ExportFormatFactory} and the rows go to the {@link TableWriter} it
 * creates, so each exporter works for every format.
 *
 * @param <T> the type of record exported
 */
public abstract class TableExporter<T> {

    /**
     * The template method. Final so every export follows the same steps.
     * Synchronized because exporters are singletons and {@link #prepare()} may
     * keep data in fields until the rows are written.
     */
    public final synchronized String export(ExportFormatFactory format) {
        prepare();
        TableWriter writer = format.createTableWriter(headers());
        for (T record : records()) {
            writer.writeRow(toRow(record));
        }
        return writer.finish();
    }

    /** Key used in the URL, e.g. "students" for {@code /api/export/students.csv}. */
    public abstract String type();

    /** Column names, in order. */
    protected abstract List<String> headers();

    /** The records to export, in the order they should appear. */
    protected abstract List<T> records();

    /** One cell per column, in the same order as {@link #headers()}. Null becomes the format's empty value. */
    protected abstract List<?> toRow(T record);

    /** Hook: runs before anything is written, e.g. to load lookup data. Does nothing by default. */
    protected void prepare() {
    }

    /** The download file name in the given format, e.g. "students.json". */
    public String fileName(ExportFormatFactory format) {
        return type() + "." + format.extension();
    }
}
