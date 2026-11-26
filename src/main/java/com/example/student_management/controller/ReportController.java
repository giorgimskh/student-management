package com.example.student_management.controller;

import com.example.student_management.export.ExportFormatFactory;
import com.example.student_management.export.ExportFormats;
import com.example.student_management.export.StudentReports;
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

/**
 * Reports built by running a visitor over the student object graph, as JSON
 * responses or as file downloads in any {@link ExportFormats export format}.
 */
@RestController
public class ReportController {
    private final StudentReportService reportService;
    private final ExportFormats formats;

    public ReportController(StudentReportService reportService, ExportFormats formats) {
        this.reportService = reportService;
        this.formats = formats;
    }

    @GetMapping("/api/reports/statistics")
    public ResponseEntity<StudentStatistics> getStatistics() {
        return ResponseEntity.ok(reportService.visitAllStudents(new StatisticsVisitor()).result());
    }

    @GetMapping("/api/reports/audit")
    public ResponseEntity<List<AuditFinding>> getAudit() {
        return ResponseEntity.ok(reportService.visitAllStudents(new StudentAuditVisitor()).result());
    }

    @GetMapping("/api/reports/statistics.{format:[^.]+}")
    public ResponseEntity<String> downloadStatistics(@PathVariable String format) {
        ExportFormatFactory factory = formats.get(format);
        StudentStatistics stats = reportService.visitAllStudents(new StatisticsVisitor()).result();
        return Downloads.attachment("statistics." + factory.extension(), factory, StudentReports.statistics(stats, factory));
    }

    @GetMapping("/api/reports/audit.{format:[^.]+}")
    public ResponseEntity<String> downloadAudit(@PathVariable String format) {
        ExportFormatFactory factory = formats.get(format);
        List<AuditFinding> findings = reportService.visitAllStudents(new StudentAuditVisitor()).result();
        return Downloads.attachment("audit." + factory.extension(), factory, StudentReports.audit(findings, factory));
    }

    @GetMapping("/api/students/{studentId}/audit")
    public ResponseEntity<List<AuditFinding>> getStudentAudit(@PathVariable UUID studentId) {
        return ResponseEntity.ok(reportService.visitStudent(studentId, new StudentAuditVisitor()).result());
    }
}
