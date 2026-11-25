package com.example.student_management.controller;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.StudentReportService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.visitor.StudentGraphVisitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
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
    void studentAudit_unknownStudent_returns404() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(reportService.visitStudent(eq(unknown), any())).thenThrow(new ResourceNotFoundException("Student not found"));

        mockMvc.perform(get("/api/students/{id}/audit", unknown))
                .andExpect(status().isNotFound());
    }

}
