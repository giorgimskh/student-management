package com.example.student_management.repository;

import com.example.student_management.TestcontainersConfiguration;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.search.StudentSearch;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The specifications {@link StudentSearch} builds, run against Postgres. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class StudentSearchRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");
    private static final PageRequest BY_NAME = PageRequest.of(0, 20, Sort.by("name"));

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Course algorithms;
    private Course databases;

    private Student student(String name, StudentStatus status, Instant createdAt, Course... courses) {
        Student student = Student.builder().name(name).email(name.toLowerCase() + "@example.com")
                .status(status).createdAt(createdAt).build();
        student.getCourses().addAll(List.of(courses));
        return entityManager.persist(student);
    }

    /** Persists a loan made with the step builder, the same way the application creates loans. */
    private void lend(Student student, String title, Instant borrowedAt, Duration period, Instant returnedAt) {
        Book book = entityManager.persist(Book.builder().title(title).isbn("9780134757599").build());
        var loan = BookLoan.lend(book).to(student).borrowedAt(borrowedAt).dueAfter(period);
        entityManager.persist(returnedAt == null ? loan.build() : loan.returnedAt(returnedAt).build());
    }

    @BeforeEach
    void setUp() {
        algorithms = entityManager.persist(Course.builder().courseName("Algorithms").code("CS101").build());
        databases = entityManager.persist(Course.builder().courseName("Databases").code("CS201").build());

        Student ana = student("Ana", StudentStatus.ACTIVE, Instant.parse("2026-01-10T00:00:00Z"), algorithms, databases);
        Student beka = student("Beka", StudentStatus.SUSPENDED, Instant.parse("2026-02-10T00:00:00Z"), algorithms);
        Student gio = student("Gio", StudentStatus.GRADUATED, Instant.parse("2026-03-10T00:00:00Z"));
        student("Nino 100%", StudentStatus.ACTIVE, Instant.parse("2026-03-15T00:00:00Z"));

        lend(ana, "Late", NOW.minus(Duration.ofDays(20)), Duration.ofDays(14), null);           // overdue
        lend(ana, "Also late", NOW.minus(Duration.ofDays(30)), Duration.ofDays(14), null);      // overdue again: Ana once
        lend(beka, "On time", NOW.minus(Duration.ofDays(3)), Duration.ofDays(14), null);        // not due yet
        lend(gio, "Returned late", NOW.minus(Duration.ofDays(40)), Duration.ofDays(14), NOW.minus(Duration.ofDays(1)));
        entityManager.flush();
        entityManager.clear();
    }

    private List<String> names(StudentSearch search) {
        return studentRepository.findAll(search.toSpecification(NOW), BY_NAME).map(Student::getName).getContent();
    }

    @Test
    void noConditions_isEveryone() {
        assertThat(names(StudentSearch.everyone())).containsExactly("Ana", "Beka", "Gio", "Nino 100%");
    }

    @Test
    void name_containsIgnoringCase_withWildcardsMatchedLiterally() {
        assertThat(names(StudentSearch.builder().nameContains("NA").build())).containsExactly("Ana");
        assertThat(names(StudentSearch.builder().nameContains("0%").build())).containsExactly("Nino 100%");
        assertThat(names(StudentSearch.builder().nameContains("%").build())).containsExactly("Nino 100%");
        assertThat(names(StudentSearch.builder().nameContains("_").build())).isEmpty();
    }

    @Test
    void email_contains() {
        assertThat(names(StudentSearch.builder().emailContains("BEKA@").build())).containsExactly("Beka");
    }

    @Test
    void severalStatuses_meanAnyOfThem() {
        assertThat(names(StudentSearch.builder().status(StudentStatus.SUSPENDED, StudentStatus.GRADUATED).build()))
                .containsExactly("Beka", "Gio");
    }

    @Test
    void enrolledIn_listsEachStudentOnce() {
        assertThat(names(StudentSearch.builder().enrolledIn(algorithms.getId()).build())).containsExactly("Ana", "Beka");
        assertThat(names(StudentSearch.builder().enrolledIn(databases.getId()).build())).containsExactly("Ana");
    }

    @Test
    void overdue_onlyCountsOpenLoansPastTheirDueDate_andListsEachStudentOnce() {
        assertThat(names(StudentSearch.builder().withOverdueLoans().build())).containsExactly("Ana");
    }

    @Test
    void createdRange_includesFrom_andExcludesBefore() {
        StudentSearch february = StudentSearch.builder()
                .createdFrom(Instant.parse("2026-02-10T00:00:00Z"))
                .createdBefore(Instant.parse("2026-03-10T00:00:00Z"))
                .build();

        assertThat(names(february)).containsExactly("Beka");
    }

    @Test
    void conditionsCombine_withAnd() {
        StudentSearch search = StudentSearch.builder()
                .enrolledIn(algorithms.getId())
                .status(StudentStatus.ACTIVE, StudentStatus.SUSPENDED)
                .withOverdueLoans()
                .build();

        assertThat(names(search)).containsExactly("Ana");
    }

    @Test
    void paging_countsStudentsNotJoinedRows() {
        Page<Student> page = studentRepository.findAll(
                StudentSearch.builder().enrolledIn(algorithms.getId()).withOverdueLoans().build().toSpecification(NOW),
                PageRequest.of(0, 1, Sort.by("name")));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Student::getName).containsExactly("Ana");
    }
}
