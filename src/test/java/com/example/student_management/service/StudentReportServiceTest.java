package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.impl.StudentReportServiceImpl;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.visitor.CsvExportVisitor;
import com.example.student_management.visitor.StatisticsVisitor;
import com.example.student_management.visitor.StudentStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs visitors over entities loaded from a real (H2) database, so the lazy collections are walked for real. */
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@DataJpaTest
@Import(StudentReportServiceImpl.class)
class StudentReportServiceTest {
    @Autowired
    private StudentReportService reportService;

    @Autowired
    private TestEntityManager entityManager;

    private UUID anaId;

    @BeforeEach
    void setUp() {
        Course algorithms = entityManager.persist(Course.builder().courseName("Algorithms").code("CS101").build());
        Student ana = entityManager.persist(Student.builder().name("Ana").email("ana@example.com").build());
        Student beka = entityManager.persist(Student.builder().name("Beka").email("beka@example.com")
                .status(StudentStatus.GRADUATED).build());
        entityManager.persist(Book.builder().title("Clean Code").student(ana).build());
        entityManager.persist(Book.builder().title("Refactoring").student(ana).build());
        entityManager.persist(Book.builder().title("Unassigned").build());
        ana.getCourses().add(algorithms);
        beka.getCourses().add(algorithms);
        entityManager.flush();
        entityManager.clear(); // so the visit loads books and courses from the database
        anaId = ana.getId();
    }

    @Test
    void visitAllStudents_walksLazyBooksAndCourses() {
        StudentStatistics stats = reportService.visitAllStudents(new StatisticsVisitor()).result();

        assertThat(stats.totalStudents()).isEqualTo(2);
        assertThat(stats.booksOnLoan()).isEqualTo(2); // the unassigned book is not reachable from a student
        assertThat(stats.enrollments()).isEqualTo(2);
        assertThat(stats.studentsByStatus()).containsEntry(StudentStatus.GRADUATED, 1L);
        assertThat(stats.enrollmentsByCourse()).singleElement()
                .satisfies(c -> assertThat(c.students()).isEqualTo(2));
    }

    @Test
    void visitAllStudents_visitsInNameOrder() {
        String csv = reportService.visitAllStudents(new CsvExportVisitor()).result();

        assertThat(csv.lines().skip(1).map(line -> line.substring(0, line.indexOf(','))))
                .containsExactly("Ana", "Beka");
    }

    @Test
    void visitStudent_visitsOnlyThatStudent() {
        StudentStatistics stats = reportService.visitStudent(anaId, new StatisticsVisitor()).result();

        assertThat(stats.totalStudents()).isEqualTo(1);
        assertThat(stats.booksOnLoan()).isEqualTo(2);
    }

    @Test
    void visitStudent_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> reportService.visitStudent(UUID.randomUUID(), new StatisticsVisitor()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
