package com.example.student_management.export;

import com.example.student_management.state.StudentStatus;
import com.example.student_management.visitor.AuditFinding;
import com.example.student_management.visitor.StudentStatistics;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The same report written by each format family. */
class StudentReportsTest {
    private static final UUID STUDENT = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOOK = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID COURSE = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private final StudentStatistics stats = new StudentStatistics(3, byStatus(), 2, 4, 1, 0.67, 1.33,
            List.of(new StudentStatistics.CourseEnrollment(COURSE, "Algorithms, Part I", "CS101", 2)));
    private final List<AuditFinding> findings = List.of(
            new AuditFinding(AuditFinding.Rule.BOOK_NOT_RETURNED, STUDENT, "Beka", BOOK, "Graduated but still has Clean Code"));

    private static Map<StudentStatus, Long> byStatus() {
        Map<StudentStatus, Long> map = new LinkedHashMap<>();
        map.put(StudentStatus.ACTIVE, 2L);
        map.put(StudentStatus.GRADUATED, 1L);
        return map;
    }

    @Test
    void statistics_asCsv() {
        assertThat(StudentReports.statistics(stats, new CsvFormatFactory()).split("\r\n", -1)).containsExactly(
                "report,statistics",
                "total_students,3",
                "books_on_loan,2",
                "enrollments,4",
                "students_with_photo,1",
                "average_books_per_student,0.67",
                "average_courses_per_student,1.33",
                "",
                "students_by_status",
                "status,students",
                "ACTIVE,2",
                "GRADUATED,1",
                "",
                "enrollments_by_course",
                "course_id,course,code,students",
                COURSE + ",\"Algorithms, Part I\",CS101,2",
                "");
    }

    @Test
    void statistics_asJson() {
        JsonNode report = JsonMapper.builder().build().readTree(StudentReports.statistics(stats, JsonFormatFactoryTest.factory()));

        assertThat(report.get("report").asString()).isEqualTo("statistics");
        assertThat(report.get("total_students").asLong()).isEqualTo(3);
        assertThat(report.get("average_books_per_student").asDouble()).isEqualTo(0.67);
        assertThat(report.get("students_by_status")).hasSize(2);
        assertThat(report.get("students_by_status").get(1).get("status").asString()).isEqualTo("GRADUATED");
        assertThat(report.get("enrollments_by_course").get(0).get("course").asString()).isEqualTo("Algorithms, Part I");
    }

    @Test
    void audit_asCsv() {
        assertThat(StudentReports.audit(findings, new CsvFormatFactory()).split("\r\n", -1)).containsExactly(
                "report,audit",
                "total_findings,1",
                "",
                "findings",
                "rule,student_id,student,related_id,detail",
                "BOOK_NOT_RETURNED," + STUDENT + ",Beka," + BOOK + ",Graduated but still has Clean Code",
                "");
    }

    @Test
    void audit_asJson_withNoFindings() {
        JsonNode report = JsonMapper.builder().build().readTree(StudentReports.audit(List.of(), JsonFormatFactoryTest.factory()));

        assertThat(report.get("total_findings").asInt()).isZero();
        assertThat(report.get("findings").isArray()).isTrue();
        assertThat(report.get("findings")).isEmpty();
    }
}
