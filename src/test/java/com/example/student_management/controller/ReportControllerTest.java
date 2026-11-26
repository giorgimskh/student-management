package com.example.student_management.controller;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.export.CsvFormatFactory;
import com.example.student_management.export.ExportFormats;
import com.example.student_management.export.JsonFormatFactory;
import com.example.student_management.service.StudentReportService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.visitor.StudentGraphVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import({ExportFormats.class, CsvFormatFactory.class, JsonFormatFactory.class})
class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentReportService reportService;

    private final UUID graduatedId = UUID.randomUUID();
    private List<Student> students;

    @BeforeEach
    void setUp() {
        Student ana = Student.builder().id(UUID.randomUUID()).name("Ana").email("ana@example.com").build();
        Student beka = Student.builder().id(graduatedId).name("Beka").email("beka@example.com")
                .status(StudentStatus.GRADUATED).build();
        beka.getBooks().add(Book.builder().id(UUID.randomUUID()).title("Clean Code").student(beka).build());
        students = List.of(ana, beka);

        // the mocked service runs the controller's visitor over in-memory students
        when(reportService.visitAllStudents(any())).thenAnswer(inv -> {
            StudentGraphVisitor visitor = inv.getArgument(0);
            students.forEach(s -> s.accept(visitor));
            return visitor;
        });
    }

    @Test
    void statistics_returnsCounts() throws Exception {
        mockMvc.perform(get("/api/reports/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents").value(2))
                .andExpect(jsonPath("$.studentsByStatus.GRADUATED").value(1))
                .andExpect(jsonPath("$.booksOnLoan").value(1));
    }

    @Test
    void audit_returnsFindings() throws Exception {
        mockMvc.perform(get("/api/reports/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rule").value("BOOK_NOT_RETURNED"))
                .andExpect(jsonPath("$[0].studentName").value("Beka"));
    }

    @Test
    void statistics_downloadsAsCsv() throws Exception {
        mockMvc.perform(get("/api/reports/statistics.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"statistics.csv\""))
                .andExpect(content().string(startsWith("report,statistics\r\ntotal_students,2\r\n")));
    }

    @Test
    void audit_downloadsAsJson() throws Exception {
        mockMvc.perform(get("/api/reports/audit.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit.json\""))
                .andExpect(jsonPath("$.report").value("audit"))
                .andExpect(jsonPath("$.total_findings").value(1))
                .andExpect(jsonPath("$.findings[0].rule").value("BOOK_NOT_RETURNED"))
                .andExpect(jsonPath("$.findings[0].student").value("Beka"));
    }

    @Test
    void reportDownload_unknownFormat_returns404() throws Exception {
        mockMvc.perform(get("/api/reports/audit.pdf"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No export format 'pdf'. Available: csv, json"));
    }

    @Test
    void studentAudit_unknownStudent_returns404() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(reportService.visitStudent(eq(unknown), any())).thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(get("/api/students/{id}/audit", unknown))
                .andExpect(status().isNotFound());
    }
}
