package com.example.student_management.controller;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.policy.NotifyOnlyPolicy;
import com.example.student_management.service.LoanService;
import com.example.student_management.service.OverdueLoanJob;
import com.example.student_management.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
@Import(LoanControllerTest.FixedClock.class)
class LoanControllerTest {
    static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private OverdueLoanJob overdueLoanJob;

    private static BookLoan loan(String title, Instant dueAt) {
        return BookLoan.builder().id(UUID.randomUUID())
                .book(Book.builder().id(UUID.randomUUID()).title(title).build())
                .student(Student.builder().id(UUID.randomUUID()).name("Nino").build())
                .borrowedAt(dueAt.minus(Duration.ofDays(14))).dueAt(dueAt).build();
    }

    @Test
    void activeLoans_byDefault_withOverdueFlag() throws Exception {
        when(loanService.activeLoans()).thenReturn(List.of(
                loan("Late", NOW.minus(Duration.ofDays(2))),
                loan("Fine", NOW.plus(Duration.ofDays(5)))));

        mockMvc.perform(get("/api/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookTitle").value("Late"))
                .andExpect(jsonPath("$[0].studentName").value("Nino"))
                .andExpect(jsonPath("$[0].overdue").value(true))
                .andExpect(jsonPath("$[1].overdue").value(false));
        verify(loanService, never()).overdueLoans();
    }

    @Test
    void overdueFilter() throws Exception {
        when(loanService.overdueLoans()).thenReturn(List.of(loan("Late", NOW.minus(Duration.ofDays(2)))));

        mockMvc.perform(get("/api/loans").param("filter", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownFilter_returns400() throws Exception {
        mockMvc.perform(get("/api/loans").param("filter", "SOMETIMES"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentLoans_404ForUnknownStudent() throws Exception {
        UUID id = UUID.randomUUID();
        when(studentService.getStudentById(id)).thenThrow(new ResourceNotFoundException("Student not found with id: " + id));

        mockMvc.perform(get("/api/students/{id}/loans", id))
                .andExpect(status().isNotFound());
        verifyNoInteractions(loanService);
    }

    @Test
    void overdueCheck_canBeTriggeredByHand_andReportsPolicyActions() throws Exception {
        when(overdueLoanJob.runCheck()).thenReturn(
                new OverdueLoanJob.Result(3, "suspend", List.of("Suspended Nino: \"Late\" is overdue")));

        mockMvc.perform(post("/api/loans/overdue-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newlyOverdue").value(3))
                .andExpect(jsonPath("$.policy").value("suspend"))
                .andExpect(jsonPath("$.actions[0]").value("Suspended Nino: \"Late\" is overdue"));
    }

    @Test
    void policyEndpoint_describesTheActiveStrategy() throws Exception {
        when(overdueLoanJob.policy()).thenReturn(new NotifyOnlyPolicy());

        mockMvc.perform(get("/api/loans/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("notify"))
                .andExpect(jsonPath("$.description").value("Overdue books are reported in the activity feed."));
    }
}
