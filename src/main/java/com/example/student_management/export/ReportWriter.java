package com.example.student_management.export;

import java.util.List;

/**
 * Product of {@link ExportFormatFactory}: writes a report made of single named
 * values followed by named tables. Values and sections appear in the order
 * they are added. A writer is used once and ends with {@link #finish()}.
 */
public interface ReportWriter {
    /** A single named value, e.g. {@code total_students = 42}. */
    ReportWriter field(String name, Object value);

    /**
     * A named table. Each row has one cell per header.
     *
     * @throws IllegalArgumentException if a row does not have one cell per header
     */
    ReportWriter section(String name, List<String> headers, List<? extends List<?>> rows);

    /** The finished document. */
    String finish();
}
