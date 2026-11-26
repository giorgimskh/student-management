package com.example.student_management.export;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The JSON family. A table is an array with one object per row, keyed by
 * column name. A report is one object: {@code "report"} holds the title, each
 * field is a property and each section is an array of row objects. Values keep
 * their JSON type (numbers, booleans, null); ids, enums and instants are
 * written as strings, instants in ISO-8601.
 *
 * <p>Uses the application's {@link JsonMapper}, so the files match the API's
 * JSON responses.
 */
@Component
public class JsonFormatFactory implements ExportFormatFactory {
    private final JsonMapper jsonMapper;

    public JsonFormatFactory(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String extension() {
        return "json";
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public TableWriter createTableWriter(List<String> headers) {
        return new JsonTableWriter(List.copyOf(headers));
    }

    @Override
    public ReportWriter createReportWriter(String title) {
        return new JsonReportWriter(title);
    }

    private String write(Object document) {
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
    }

    private static Map<String, Object> toObject(List<String> headers, List<?> cells) {
        Rows.requireColumns(cells, headers.size());
        Map<String, Object> object = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            object.put(headers.get(i), cells.get(i));
        }
        return object;
    }

    private final class JsonTableWriter implements TableWriter {
        private final List<String> headers;
        private final List<Map<String, Object>> rows = new ArrayList<>();

        JsonTableWriter(List<String> headers) {
            this.headers = headers;
        }

        @Override
        public void writeRow(List<?> cells) {
            rows.add(toObject(headers, cells));
        }

        @Override
        public String finish() {
            return write(rows);
        }
    }

    private final class JsonReportWriter implements ReportWriter {
        private final Map<String, Object> report = new LinkedHashMap<>();

        JsonReportWriter(String title) {
            report.put("report", title);
        }

        @Override
        public ReportWriter field(String name, Object value) {
            report.put(name, value);
            return this;
        }

        @Override
        public ReportWriter section(String name, List<String> headers, List<? extends List<?>> rows) {
            report.put(name, rows.stream().map(row -> toObject(headers, row)).toList());
            return this;
        }

        @Override
        public String finish() {
            return write(report);
        }
    }
}
