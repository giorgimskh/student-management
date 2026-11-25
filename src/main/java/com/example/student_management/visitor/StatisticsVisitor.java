package com.example.student_management.visitor;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.state.StudentStatus;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Counts students per status, books on loan, enrollments per course and photos.
 * Only what is reachable from students is seen: unassigned books and empty
 * courses are not counted.
 */
public class StatisticsVisitor implements StudentGraphVisitor {
    private final Map<StudentStatus, Long> byStatus = new EnumMap<>(StudentStatus.class);
    private final Map<UUID, CourseTally> courses = new LinkedHashMap<>();
    private long students;
    private long books;
    private long enrollments;
    private long photos;

    public StatisticsVisitor() {
        for (StudentStatus status : StudentStatus.values()) {
            byStatus.put(status, 0L);
        }
    }

    @Override
    public void visit(Student student) {
        students++;
        byStatus.merge(student.getStatus(), 1L, Long::sum);
    }

    @Override
    public void visit(Book book) {
        books++;
    }

    @Override
    public void visit(Course course) {
        enrollments++;
        courses.computeIfAbsent(course.getId(), id -> new CourseTally(course.getCourseName(), course.getCode())).students++;
    }

    @Override
    public void visit(StudentPhoto photo) {
        photos++;
    }

    public StudentStatistics result() {
        List<StudentStatistics.CourseEnrollment> perCourse = courses.entrySet().stream()
                .map(e -> new StudentStatistics.CourseEnrollment(
                        e.getKey(), e.getValue().name, e.getValue().code, e.getValue().students))
                .sorted(Comparator.comparingLong(StudentStatistics.CourseEnrollment::students).reversed()
                        .thenComparing(c -> String.valueOf(c.courseName())))
                .toList();
        return new StudentStatistics(
                students,
                Map.copyOf(byStatus),
                books,
                enrollments,
                photos,
                average(books),
                average(enrollments),
                perCourse);
    }

    private double average(long total) {
        return students == 0 ? 0 : Math.round(total * 100.0 / students) / 100.0;
    }

    private static final class CourseTally {
        private final String name;
        private final String code;
        private long students;

        private CourseTally(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }
}
