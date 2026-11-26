package com.example.student_management.export;

import java.util.List;

/**
 * Product of {@link ExportFormatFactory}: writes one table, row by row. The
 * column names are given when the writer is created. A writer is used once:
 * add the rows, then call {@link #finish()}.
 */
public interface TableWriter {
    /**
     * One cell per column, in column order. A null cell is written as the
     * format's empty value.
     *
     * @throws IllegalArgumentException if the row does not have one cell per column
     */
    void writeRow(List<?> cells);

    /** The finished document. */
    String finish();
}
