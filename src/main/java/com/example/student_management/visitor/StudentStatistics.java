package com.example.student_management.visitor;

import com.example.student_management.state.StudentStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Result of {@link StatisticsVisitor}. Averages are per student, rounded to 2 decimals. */
public record StudentStatistics(
        long totalStudents,
        Map<StudentStatus, Long> studentsByStatus,
        long booksOnLoan,
        long enrollments,
        long studentsWithPhoto,
        double averageBooksPerStudent,
        double averageCoursesPerStudent,
        List<CourseEnrollment> enrollmentsByCourse
) {
    /** How many students are enrolled in one course; most popular first. */
    public record CourseEnrollment(UUID courseId, String courseName, String code, long students) {
    }
}
