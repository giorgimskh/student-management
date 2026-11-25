package com.example.student_management.controller;

import com.example.student_management.service.StudentReportService;
import com.example.student_management.visitor.AuditFinding;
import com.example.student_management.visitor.StatisticsVisitor;
import com.example.student_management.visitor.StudentAuditVisitor;
import com.example.student_management.visitor.StudentStatistics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Reports built by running a visitor over the student object graph. */
@RestController
public class ReportController {
    private final StudentReportService reportService;

    public ReportController(StudentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/reports/statistics")
    public ResponseEntity<StudentStatistics> getStatistics() {
        return ResponseEntity.ok(reportService.visitAllStudents(new StatisticsVisitor()).result());
    }

    @GetMapping("/api/reports/audit")
    public ResponseEntity<List<AuditFinding>> getAudit() {
        return ResponseEntity.ok(reportService.visitAllStudents(new StudentAuditVisitor()).result());
    }

    @GetMapping("/api/students/{studentId}/audit")
    public ResponseEntity<List<AuditFinding>> getStudentAudit(@PathVariable UUID studentId) {
        return ResponseEntity.ok(reportService.visitStudent(studentId, new StudentAuditVisitor()).result());
    }

}
