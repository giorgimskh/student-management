package com.example.student_management.controller;

import com.example.student_management.export.CsvExporter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
@Import(ExportControllerTest.Exporters.class)
class ExportControllerTest {

    static CsvExporter<String> exporter(String type) {
        return new CsvExporter<>() {
            @Override
            public String type() {
                return type;
            }

            @Override
            protected List<String> headers() {
                return List.of("name");
            }

            @Override
            protected List<String> records() {
                return List.of("Ana");
            }

            @Override
            protected List<?> toRow(String record) {
                return List.of(record);
            }
        };
    }

    @TestConfiguration
    static class Exporters {
        @Bean
        CsvExporter<String> widgets() {
            return exporter("widgets");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void downloadsCsvAsAnAttachment() throws Exception {
        mockMvc.perform(get("/api/export/widgets.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"widgets.csv\""))
                .andExpect(content().string("name\r\nAna\r\n"));
    }

    @Test
    void unknownExport_is404_andListsWhatExists() throws Exception {
        mockMvc.perform(get("/api/export/secrets.csv"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No CSV export 'secrets'. Available: widgets"));
    }

    @Test
    void twoExportersWithTheSameType_failAtStartup() {
        assertThatThrownBy(() -> new ExportController(List.of(exporter("x"), exporter("x"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'x'");
    }
}
