package com.example.student_management.export;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The Template Method itself, with a minimal subclass and a format that records what it is given. */
class TableExporterTest {

    /** Records the order the template calls its steps in, in the same list as the writer calls. */
    static class RecordingExporter extends TableExporter<List<Object>> {
        final List<String> calls;
        private final List<List<Object>> rows;

        RecordingExporter(List<String> calls, List<List<Object>> rows) {
            this.calls = calls;
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

    /** A format whose table writer logs every call and returns the log as the document. */
    static class RecordingFormat implements ExportFormatFactory {
        final List<String> calls;

        RecordingFormat(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public String extension() {
            return "log";
        }

        @Override
        public MediaType mediaType() {
            return MediaType.TEXT_PLAIN;
        }

        @Override
        public TableWriter createTableWriter(List<String> headers) {
            calls.add("create " + headers);
            return new TableWriter() {
                @Override
                public void writeRow(List<?> cells) {
                    calls.add("write " + cells);
                }

                @Override
                public String finish() {
                    calls.add("finish");
                    return String.join("|", calls);
                }
            };
        }

        @Override
        public ReportWriter createReportWriter(String title) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void runsTheStepsInOrder_andHandsEveryRowToTheFormatsWriter() {
        List<String> calls = new ArrayList<>();
        RecordingExporter exporter = new RecordingExporter(calls, List.of(List.of("x", 1), List.of("y", 2)));

        String document = exporter.export(new RecordingFormat(calls));

        assertThat(calls).containsExactly(
                "prepare", "headers", "create [a, b]", "records", "row", "write [x, 1]", "row", "write [y, 2]", "finish");
        assertThat(document).endsWith("|finish");
    }

    @Test
    void fileName_usesTheFormatsExtension() {
        RecordingExporter exporter = new RecordingExporter(new ArrayList<>(), List.of());

        assertThat(exporter.fileName(new RecordingFormat(new ArrayList<>()))).isEqualTo("things.log");
        assertThat(exporter.fileName(new CsvFormatFactory())).isEqualTo("things.csv");
    }

    @Test
    void sameExporter_writesEveryFormat() {
        RecordingExporter exporter = new RecordingExporter(new ArrayList<>(), List.of(List.of("x", 1)));

        assertThat(exporter.export(new CsvFormatFactory())).isEqualTo("a,b\r\nx,1\r\n");
        assertThat(exporter.export(JsonFormatFactoryTest.factory()).replaceAll("\\s", ""))
                .isEqualTo("[{\"a\":\"x\",\"b\":1}]");
    }
}
