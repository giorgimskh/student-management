package com.example.student_management.export;

import com.example.student_management.visitor.AuditFinding;
import com.example.student_management.visitor.StudentStatistics;

import java.util.Arrays;
import java.util.List;

/** Writes the visitor reports as downloadable files, in whichever format is passed in. */
public final class StudentReports {
    private StudentReports() {
    }

    public static String statistics(StudentStatistics stats, ExportFormatFactory format) {
        return format.createReportWriter("statistics")
                .field("total_students", stats.totalStudents())
                .field("books_on_loan", stats.booksOnLoan())
                .field("enrollments", stats.enrollments())
                .field("students_with_photo", stats.studentsWithPhoto())
                .field("average_books_per_student", stats.averageBooksPerStudent())
                .field("average_courses_per_student", stats.averageCoursesPerStudent())
                .section("students_by_status", List.of("status", "students"),
                        stats.studentsByStatus().entrySet().stream()
                                .map(e -> List.of(e.getKey(), e.getValue()))
                                .toList())
                .section("enrollments_by_course", List.of("course_id", "course", "code", "students"),
                        stats.enrollmentsByCourse().stream()
                                .map(c -> Arrays.asList(c.courseId(), c.courseName(), c.code(), c.students()))
                                .toList())
                .finish();
    }

    public static String audit(List<AuditFinding> findings, ExportFormatFactory format) {
        return format.createReportWriter("audit")
                .field("total_findings", findings.size())
                .section("findings", List.of("rule", "student_id", "student", "related_id", "detail"),
                        findings.stream()
                                .map(f -> Arrays.asList(f.rule(), f.studentId(), f.studentName(), f.relatedId(), f.detail()))
                                .toList())
                .finish();
    }
}
