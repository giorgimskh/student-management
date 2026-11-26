package com.example.student_management.export;

import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonFormatFactoryTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    static JsonFormatFactory factory() {
        return new JsonFormatFactory(MAPPER);
    }

    private final JsonFormatFactory json = factory();

    @Test
    void describesItself() {
        assertThat(json.extension()).isEqualTo("json");
        assertThat(json.mediaType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void table_isAnArrayOfObjectsKeyedByColumn_keepingJsonTypes() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        TableWriter writer = json.createTableWriter(List.of("id", "name", "status", "since", "count", "ok", "note"));
        writer.writeRow(Arrays.asList(id, "Ana", StudentStatus.ACTIVE, Instant.parse("2026-01-02T03:04:05Z"), 3, true, null));

        JsonNode rows = MAPPER.readTree(writer.finish());

        assertThat(rows.isArray()).isTrue();
        assertThat(rows).hasSize(1);
        JsonNode row = rows.get(0);
        assertThat(row.propertyNames()).containsExactly("id", "name", "status", "since", "count", "ok", "note");
        assertThat(row.get("id").asString()).isEqualTo(id.toString());
        assertThat(row.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(row.get("since").asString()).isEqualTo("2026-01-02T03:04:05Z");
        assertThat(row.get("count").isInt()).isTrue();
        assertThat(row.get("ok").isBoolean()).isTrue();
        assertThat(row.get("note").isNull()).isTrue();
    }

    @Test
    void table_withNoRows_isAnEmptyArray() {
        assertThat(json.createTableWriter(List.of("a")).finish().replaceAll("\\s", "")).isEqualTo("[]");
    }

    @Test
    void table_rejectsARowWithTheWrongNumberOfCells() {
        TableWriter writer = json.createTableWriter(List.of("a", "b"));

        assertThatThrownBy(() -> writer.writeRow(List.of("only one")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void report_isOneObject_withFieldsAndSectionsInOrder() {
        String report = json.createReportWriter("stats")
                .field("total", 3)
                .section("by_status", List.of("status", "students"), List.of(List.of("ACTIVE", 2)))
                .finish();

        JsonNode node = MAPPER.readTree(report);
        assertThat(node.propertyNames()).containsExactly("report", "total", "by_status");
        assertThat(node.get("report").asString()).isEqualTo("stats");
        assertThat(node.get("total").asInt()).isEqualTo(3);
        assertThat(node.get("by_status").get(0).get("status").asString()).isEqualTo("ACTIVE");
        assertThat(node.get("by_status").get(0).get("students").asInt()).isEqualTo(2);
    }

    @Test
    void stringsAreEscaped_notInterpretedAsFormulasOrMarkup() {
        TableWriter writer = json.createTableWriter(List.of("name"));
        writer.writeRow(List.of("=HYPERLINK(\"x\")\n\"quoted\""));

        assertThat(MAPPER.readTree(writer.finish()).get(0).get("name").asString()).isEqualTo("=HYPERLINK(\"x\")\n\"quoted\"");
    }
}
