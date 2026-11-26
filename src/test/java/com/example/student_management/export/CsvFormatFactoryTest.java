package com.example.student_management.export;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFormatFactoryTest {
    private final CsvFormatFactory csv = new CsvFormatFactory();

    @Test
    void describesItself() {
        assertThat(csv.extension()).isEqualTo("csv");
        assertThat(csv.mediaType().toString()).isEqualTo("text/csv;charset=UTF-8");
    }

    @Test
    void table_isHeaderThenRows_withCrlf() {
        TableWriter writer = csv.createTableWriter(List.of("a", "b"));
        writer.writeRow(List.of("x", 1));
        writer.writeRow(Arrays.asList(null, "z"));

        assertThat(writer.finish()).isEqualTo("a,b\r\nx,1\r\n,z\r\n");
    }

    @Test
    void table_withNoRows_isJustTheHeader() {
        assertThat(csv.createTableWriter(List.of("a", "b")).finish()).isEqualTo("a,b\r\n");
    }

    @Test
    void table_rejectsARowWithTheWrongNumberOfCells() {
        TableWriter writer = csv.createTableWriter(List.of("a", "b"));

        assertThatThrownBy(() -> writer.writeRow(List.of("only one")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Row has 1 cells but there are 2 columns");
    }

    @Test
    void report_isFieldsThenSectionsSeparatedByBlankLines() {
        String report = csv.createReportWriter("stats")
                .field("total", 3)
                .field("note", null)
                .section("by_status", List.of("status", "students"), List.of(List.of("ACTIVE", 2), List.of("GRADUATED", 1)))
                .section("empty", List.of("x"), List.of())
                .finish();

        assertThat(report).isEqualTo("""
                report,stats\r
                total,3\r
                note,\r
                \r
                by_status\r
                status,students\r
                ACTIVE,2\r
                GRADUATED,1\r
                \r
                empty\r
                x\r
                """);
    }

    @Test
    void escapesCommasQuotesAndNewlines() {
        assertThat(CsvFormatFactory.escape("Smith, Ana")).isEqualTo("\"Smith, Ana\"");
        assertThat(CsvFormatFactory.escape("The \"Best\" Book")).isEqualTo("\"The \"\"Best\"\" Book\"");
        assertThat(CsvFormatFactory.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvFormatFactory.escape("plain")).isEqualTo("plain");
    }

    @Test
    void nullsAreEmpty_andInstantsAreIso8601() {
        assertThat(CsvFormatFactory.escape(null)).isEmpty();
        assertThat(CsvFormatFactory.escape(Instant.parse("2026-01-02T03:04:05Z"))).isEqualTo("2026-01-02T03:04:05Z");
    }

    @Test
    void cellsThatLookLikeSpreadsheetFormulas_areNeutralised() {
        assertThat(CsvFormatFactory.escape("=HYPERLINK(\"http://evil\")")).isEqualTo("\"'=HYPERLINK(\"\"http://evil\"\")\"");
        assertThat(CsvFormatFactory.escape("+1")).isEqualTo("'+1");
        assertThat(CsvFormatFactory.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvFormatFactory.escape(-5)).isEqualTo("-5"); // real numbers are left alone
    }
}
