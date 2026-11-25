package com.example.student_management.repository;

import com.example.student_management.TestcontainersConfiguration;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BookLoanRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    @Autowired
    private BookLoanRepository loanRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Student student;

    @BeforeEach
    void setUp() {
        student = entityManager.persist(Student.builder().name("Nino").email("nino@example.com").build());
    }

    private BookLoan loan(String title, Instant borrowedAt, Duration period, Instant returnedAt) {
        Book book = entityManager.persist(Book.builder().title(title).isbn("9780134757599").build());
        return entityManager.persist(BookLoan.builder().book(book).student(student)
                .borrowedAt(borrowedAt).dueAt(borrowedAt.plus(period)).returnedAt(returnedAt).build());
    }

    @Test
    void activeAndOverdueQueries() {
        loan("due soon", NOW.minus(Duration.ofDays(10)), Duration.ofDays(14), null);
        loan("overdue", NOW.minus(Duration.ofDays(20)), Duration.ofDays(14), null);
        loan("returned", NOW.minus(Duration.ofDays(30)), Duration.ofDays(14), NOW.minus(Duration.ofDays(1)));
        entityManager.flush();
        entityManager.clear();

        assertThat(loanRepository.findByReturnedAtIsNullOrderByDueAtAsc())
                .extracting(l -> l.getBook().getTitle()).containsExactly("overdue", "due soon");
        assertThat(loanRepository.findByReturnedAtIsNullAndDueAtBeforeOrderByDueAtAsc(NOW))
                .extracting(l -> l.getBook().getTitle()).containsExactly("overdue");
        assertThat(loanRepository.findByStudentIdOrderByBorrowedAtDesc(student.getId()))
                .extracting(l -> l.getBook().getTitle()).containsExactly("due soon", "overdue", "returned");
    }

    @Test
    void notYetReportedOverdue_excludesLoansAlreadyReported() {
        BookLoan reported = loan("reported", NOW.minus(Duration.ofDays(20)), Duration.ofDays(14), null);
        reported.setOverdueNotifiedAt(NOW.minus(Duration.ofDays(1)));
        loan("new", NOW.minus(Duration.ofDays(15)), Duration.ofDays(14), null);
        entityManager.flush();

        assertThat(loanRepository.findByReturnedAtIsNullAndDueAtBeforeAndOverdueNotifiedAtIsNull(NOW))
                .extracting(l -> l.getBook().getTitle()).containsExactly("new");
    }

    @Test
    void deletingTheStudent_deletesTheirLoanHistory() {
        BookLoan loan = loan("book", NOW, Duration.ofDays(14), NOW);
        entityManager.flush();
        entityManager.clear();

        entityManager.getEntityManager().createNativeQuery("delete from students where id = :id")
                .setParameter("id", student.getId()).executeUpdate(); // relies on ON DELETE CASCADE from V3

        assertThat(loanRepository.findById(loan.getId())).isEmpty();
    }
}
