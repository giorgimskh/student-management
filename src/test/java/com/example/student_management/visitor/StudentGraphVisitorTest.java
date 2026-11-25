package com.example.student_management.visitor;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class StudentGraphVisitorTest {
    private final Course algorithms = course("Algorithms", "CS101");
    private final Course databases = course("Databases", "CS202");

    private static Course course(String name, String code) {
        return Course.builder().id(UUID.randomUUID()).courseName(name).code(code).build();
    }

    private static Student student(String name, StudentStatus status, List<String> bookTitles, Course... courses) {
        Student student = Student.builder().id(UUID.randomUUID()).name(name)
                .email(name.toLowerCase() + "@example.com").status(status).build();
        bookTitles.forEach(title -> student.getBooks().add(
                Book.builder().id(UUID.randomUUID()).title(title).student(student).build()));
        student.getCourses().addAll(List.of(courses));
        return student;
    }

    private static StudentPhoto photo(int recordedSize, int actualSize) {
        return StudentPhoto.builder().id(UUID.randomUUID()).data(new byte[actualSize]).sizeBytes(recordedSize)
                .contentType("image/png").build();
    }

    private static <V extends StudentGraphVisitor> V visitAll(V visitor, Student... students) {
        for (Student s : students) {
            s.accept(visitor);
        }
        return visitor;
    }

    // --- traversal ---

    @Test
    void accept_visitsStudentFirst_thenBooksCoursesAndPhoto() {
        Student ana = student("Ana", StudentStatus.ACTIVE, List.of("Clean Code"), algorithms);
        ana.setStudentPhoto(photo(3, 3));
        List<String> visited = new ArrayList<>();

        ana.accept(new StudentGraphVisitor() {
            public void visit(Student s) { visited.add("student " + s.getName()); }
            public void visit(Book b) { visited.add("book " + b.getTitle()); }
            public void visit(Course c) { visited.add("course " + c.getCode()); }
            public void visit(StudentPhoto p) { visited.add("photo"); }
        });

        assertThat(visited).containsExactly("student Ana", "book Clean Code", "course CS101", "photo");
    }

    // --- StatisticsVisitor ---

    @Test
    void statistics_countsAcrossStudents() {
        Student ana = student("Ana", StudentStatus.ACTIVE, List.of("A", "B"), algorithms, databases);
        ana.setStudentPhoto(photo(1, 1));
        Student beka = student("Beka", StudentStatus.GRADUATED, List.of("C"), algorithms);
        Student dato = student("Dato", StudentStatus.ACTIVE, List.of());

        StudentStatistics stats = visitAll(new StatisticsVisitor(), ana, beka, dato).result();

        assertThat(stats.totalStudents()).isEqualTo(3);
        assertThat(stats.studentsByStatus()).containsEntry(StudentStatus.ACTIVE, 2L)
                .containsEntry(StudentStatus.GRADUATED, 1L)
                .containsEntry(StudentStatus.SUSPENDED, 0L)
                .containsEntry(StudentStatus.WITHDRAWN, 0L);
        assertThat(stats.booksOnLoan()).isEqualTo(3);
        assertThat(stats.enrollments()).isEqualTo(3);
        assertThat(stats.studentsWithPhoto()).isEqualTo(1);
        assertThat(stats.averageBooksPerStudent()).isEqualTo(1.0);
        assertThat(stats.averageCoursesPerStudent()).isEqualTo(1.0);
        assertThat(stats.enrollmentsByCourse()).extracting(StudentStatistics.CourseEnrollment::code, StudentStatistics.CourseEnrollment::students)
                .containsExactly(tuple("CS101", 2L), tuple("CS202", 1L));
    }

    @Test
    void statistics_withNoStudents_isAllZero() {
        StudentStatistics stats = new StatisticsVisitor().result();

        assertThat(stats.totalStudents()).isZero();
        assertThat(stats.averageBooksPerStudent()).isZero();
        assertThat(stats.enrollmentsByCourse()).isEmpty();
    }

    // --- CsvExportVisitor ---

    @Test
    void csv_hasOneRowPerStudent_withSortedLists() {
        Student ana = student("Ana", StudentStatus.ACTIVE, List.of("Refactoring", "Clean Code"), databases, algorithms);
        ana.setStudentPhoto(photo(1, 1));
        Student beka = student("Beka", StudentStatus.SUSPENDED, List.of());

        String csv = visitAll(new CsvExportVisitor(), ana, beka).result();

        assertThat(csv).isEqualTo(
                CsvExportVisitor.HEADER + "\r\n"
                + "Ana,ana@example.com,ACTIVE,Clean Code; Refactoring,CS101; CS202,true\r\n"
                + "Beka,beka@example.com,SUSPENDED,,,false\r\n");
    }

    @Test
    void csv_quotesSpecialCharacters_andDefusesFormulas() {
        assertThat(CsvExportVisitor.cell("Smith, John")).isEqualTo("\"Smith, John\"");
        assertThat(CsvExportVisitor.cell("The \"Book\"")).isEqualTo("\"The \"\"Book\"\"\"");
        assertThat(CsvExportVisitor.cell("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(CsvExportVisitor.cell("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvExportVisitor.cell(null)).isEmpty();
    }

    // --- StudentAuditVisitor ---

    @Test
    void audit_flagsBooksHeldByGraduatedAndWithdrawnStudents() {
        Student graduated = student("Ana", StudentStatus.GRADUATED, List.of("Clean Code"));
        Student withdrawn = student("Beka", StudentStatus.WITHDRAWN, List.of("SICP"));
        Student suspended = student("Dato", StudentStatus.SUSPENDED, List.of("TAOCP"));

        List<AuditFinding> findings = visitAll(new StudentAuditVisitor(), graduated, withdrawn, suspended).result();

        assertThat(findings).extracting(AuditFinding::rule, AuditFinding::studentName).containsExactly(
                tuple(AuditFinding.Rule.BOOK_NOT_RETURNED, "Ana"),
                tuple(AuditFinding.Rule.BOOK_NOT_RETURNED, "Beka"));
        assertThat(findings.getFirst().detail()).isEqualTo("\"Clean Code\" is still on loan to a GRADUATED student");
        assertThat(findings.getFirst().relatedId()).isEqualTo(graduated.getBooks().getFirst().getId());
    }

    @Test
    void audit_flagsCoursesOfWithdrawnStudentsOnly() {
        Student withdrawn = student("Beka", StudentStatus.WITHDRAWN, List.of(), algorithms);
        Student graduated = student("Ana", StudentStatus.GRADUATED, List.of(), algorithms);

        List<AuditFinding> findings = visitAll(new StudentAuditVisitor(), withdrawn, graduated).result();

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.rule()).isEqualTo(AuditFinding.Rule.ENROLLED_AFTER_WITHDRAWAL);
            assertThat(f.studentId()).isEqualTo(withdrawn.getId());
            assertThat(f.relatedId()).isEqualTo(algorithms.getId());
        });
    }

    @Test
    void audit_flagsPhotoSizeMismatch() {
        Student ok = student("Ana", StudentStatus.ACTIVE, List.of());
        ok.setStudentPhoto(photo(4, 4));
        Student broken = student("Beka", StudentStatus.ACTIVE, List.of());
        broken.setStudentPhoto(photo(10, 4));

        List<AuditFinding> findings = visitAll(new StudentAuditVisitor(), ok, broken).result();

        assertThat(findings).singleElement().satisfies(f -> {
            assertThat(f.rule()).isEqualTo(AuditFinding.Rule.PHOTO_SIZE_MISMATCH);
            assertThat(f.studentName()).isEqualTo("Beka");
            assertThat(f.detail()).isEqualTo("Photo is recorded as 10 bytes but has 4");
        });
    }

    @Test
    void audit_isEmpty_forActiveStudentsInGoodShape() {
        Student ana = student("Ana", StudentStatus.ACTIVE, List.of("Clean Code"), algorithms);

        assertThat(visitAll(new StudentAuditVisitor(), ana).result()).isEmpty();
    }

    @Test
    void courseLabel_copesWithMissingNameOrCode() {
        assertThat(course("Algorithms", "CS101").label()).isEqualTo("Algorithms (CS101)");
        assertThat(course(null, "NET101").label()).isEqualTo("NET101");
        assertThat(course("Algorithms", " ").label()).isEqualTo("Algorithms");
        Course neither = course(null, null);
        assertThat(neither.label()).isEqualTo("course " + neither.getId());
    }

    @Test
    void audit_usesCourseLabel_whenNameIsMissing() {
        Student withdrawn = student("Beka", StudentStatus.WITHDRAWN, List.of(), course(null, "NET101"));

        assertThat(visitAll(new StudentAuditVisitor(), withdrawn).result())
                .singleElement().extracting(AuditFinding::detail).isEqualTo("Still enrolled in NET101");
    }
}
