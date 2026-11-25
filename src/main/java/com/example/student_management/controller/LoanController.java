package com.example.student_management.controller;

import com.example.student_management.domain.BookLoan;
import com.example.student_management.dto.LoanResponse;
import com.example.student_management.service.LoanService;
import com.example.student_management.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class LoanController {
    /** Which loans {@code GET /api/loans} returns. */
    public enum LoanFilter { ACTIVE, OVERDUE }

    private final LoanService loanService;
    private final StudentService studentService;
    private final Clock clock;

    public LoanController(LoanService loanService, StudentService studentService, Clock clock) {
        this.loanService = loanService;
        this.studentService = studentService;
        this.clock = clock;
    }

    /** Books currently borrowed ({@code ?filter=ACTIVE}, the default) or only the overdue ones. */
    @GetMapping("/api/loans")
    public ResponseEntity<List<LoanResponse>> getLoans(@RequestParam(defaultValue = "ACTIVE") LoanFilter filter) {
        List<BookLoan> loans = filter == LoanFilter.OVERDUE ? loanService.overdueLoans() : loanService.activeLoans();
        return ResponseEntity.ok(toResponses(loans));
    }

    /** Every loan of one student, newest first, including books already given back. */
    @GetMapping("/api/students/{studentId}/loans")
    public ResponseEntity<List<LoanResponse>> getStudentLoans(@PathVariable UUID studentId) {
        studentService.getStudentById(studentId); // 404 for unknown students
        return ResponseEntity.ok(toResponses(loanService.loansForStudent(studentId)));
    }

    /** Runs the overdue check now instead of waiting for the schedule. */
    @PostMapping("/api/loans/overdue-check")
    public ResponseEntity<Map<String, Integer>> runOverdueCheck() {
        return ResponseEntity.ok(Map.of("newlyOverdue", loanService.reportNewlyOverdueLoans()));
    }

    private List<LoanResponse> toResponses(List<BookLoan> loans) {
        Instant now = Instant.now(clock);
        return loans.stream().map(loan -> LoanResponse.from(loan, now)).toList();
    }
}
