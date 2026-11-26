package com.example.student_management.controller;

import com.example.student_management.export.CsvFormatFactory;
import com.example.student_management.export.ExportFormats;
import com.example.student_management.export.JsonFormatFactory;
import com.example.student_management.export.TableExporter;
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
@Import({ExportControllerTest.Exporters.class, ExportFormats.class, CsvFormatFactory.class, JsonFormatFactory.class})
class ExportControllerTest {

    static TableExporter<String> exporter(String type) {
        return new TableExporter<>() {
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
        TableExporter<String> widgets() {
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
    void downloadsTheSameExportAsJson() throws Exception {
        mockMvc.perform(get("/api/export/widgets.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"widgets.json\""))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ana"));
    }

    @Test
    void unknownExport_is404_andListsWhatExists() throws Exception {
        mockMvc.perform(get("/api/export/secrets.csv"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No export 'secrets'. Available: widgets"));
    }

    @Test
    void unknownFormat_is404_andListsWhatExists() throws Exception {
        mockMvc.perform(get("/api/export/widgets.xml"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No export format 'xml'. Available: csv, json"));
    }

    @Test
    void twoExportersWithTheSameType_failAtStartup() {
        ExportFormats formats = new ExportFormats(List.of(new CsvFormatFactory()));
        assertThatThrownBy(() -> new ExportController(List.of(exporter("x"), exporter("x")), formats))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'x'");
    }

    @Test
    void twoFormatsWithTheSameExtension_failAtStartup() {
        assertThatThrownBy(() -> new ExportFormats(List.of(new CsvFormatFactory(), new CsvFormatFactory())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("'csv'");
    }
}
