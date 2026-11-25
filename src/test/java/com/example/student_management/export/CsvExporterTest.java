package com.example.student_management.export;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The Template Method itself, with a minimal subclass. */
class CsvExporterTest {

    /** Records the order the template calls its steps in. */
    static class RecordingExporter extends CsvExporter<List<Object>> {
        final List<String> calls = new ArrayList<>();
        private final List<List<Object>> rows;

        RecordingExporter(List<List<Object>> rows) {
            this.rows = rows;
        }

        @Override
        public String type() {
            return "things";
        }

        @Override
        protected void prepare() {
            calls.add("prepare");
        }

        @Override
        protected List<String> headers() {
            calls.add("headers");
            return List.of("a", "b");
        }

        @Override
        protected List<List<Object>> records() {
            calls.add("records");
            return rows;
        }

        @Override
        protected List<?> toRow(List<Object> record) {
            calls.add("row");
            return record;
        }
    }

    @Test
    void runsTheStepsInOrder_andWritesHeaderThenRowsWithCrlf() {
        RecordingExporter exporter = new RecordingExporter(List.of(List.of("x", 1), List.of("y", 2)));

        String csv = exporter.export();

        assertThat(exporter.calls).containsExactly("prepare", "headers", "records", "row", "row");
        assertThat(csv).isEqualTo("a,b\r\nx,1\r\ny,2\r\n");
        assertThat(exporter.fileName()).isEqualTo("things.csv");
    }

    @Test
    void noRecords_givesJustTheHeader() {
        assertThat(new RecordingExporter(List.of()).export()).isEqualTo("a,b\r\n");
    }

    @Test
    void escapesCommasQuotesAndNewlines() {
        assertThat(CsvExporter.escape("Smith, Ana")).isEqualTo("\"Smith, Ana\"");
        assertThat(CsvExporter.escape("The \"Best\" Book")).isEqualTo("\"The \"\"Best\"\" Book\"");
        assertThat(CsvExporter.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvExporter.escape("plain")).isEqualTo("plain");
    }

    @Test
    void nullsAreEmpty_andInstantsAreIso8601() {
        assertThat(CsvExporter.escape(null)).isEmpty();
        assertThat(CsvExporter.escape(Instant.parse("2026-01-02T03:04:05Z"))).isEqualTo("2026-01-02T03:04:05Z");
        assertThat(new RecordingExporter(List.of(Arrays.asList(null, "z"))).export()).endsWith(",z\r\n");
    }

    @Test
    void cellsThatLookLikeSpreadsheetFormulas_areNeutralised() {
        assertThat(CsvExporter.escape("=HYPERLINK(\"http://evil\")")).isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\"");
        assertThat(CsvExporter.escape("+1")).isEqualTo("'+1");
        assertThat(CsvExporter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvExporter.escape(-5)).isEqualTo("-5"); // real numbers are left alone
    }
}
