package com.example.student_management.export;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** The three concrete exporters: each only supplies columns, records and rows. */
@ExtendWith(MockitoExtension.class)
class EntityCsvExportersTest {
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookLoanRepository loanRepository;

    private final UUID anaId = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final Student ana = Student.builder().id(anaId).name("Smith, Ana").email("ana@example.com")
            .status(StudentStatus.SUSPENDED).createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

    @Test
    void students() {
        when(studentRepository.findAll(Sort.by("name"))).thenReturn(List.of(ana));

        assertThat(new StudentCsvExporter(studentRepository).export()).isEqualTo(
                "id,name,email,status,created_at\r\n"
                        + anaId + ",\"Smith, Ana\",ana@example.com,SUSPENDED,2026-01-01T00:00:00Z\r\n");
    }

    @Test
    void books_showBorrowerAndDueDate_fromOneLoanQuery() {
        Book lent = Book.builder().id(UUID.randomUUID()).title("Clean Code").isbn("9780132350884").build();
        Book free = Book.builder().id(UUID.randomUUID()).title("Refactoring").isbn("9780134757599").build();
        BookLoan loan = BookLoan.builder().book(lent).student(ana).borrowedAt(NOW).dueAt(NOW.plus(Duration.ofDays(14))).build();
        when(bookRepository.findAll(Sort.by("title"))).thenReturn(List.of(lent, free));
        when(loanRepository.findByReturnedAtIsNullOrderByDueAtAsc()).thenReturn(List.of(loan));

        String csv = new BookCsvExporter(bookRepository, loanRepository).export();

        assertThat(csv.split("\r\n")).containsExactly(
                "id,title,isbn,borrowed_by,due_at",
                lent.getId() + ",Clean Code,9780132350884,\"Smith, Ana\",2026-04-03T10:00:00Z",
                free.getId() + ",Refactoring,9780134757599,,");
    }

    @Test
    void loans_markOverdueAgainstOneMoment() {
        Book book = Book.builder().id(UUID.randomUUID()).title("Late Book").build();
        BookLoan late = BookLoan.builder().book(book).student(ana)
                .borrowedAt(NOW.minus(Duration.ofDays(20))).dueAt(NOW.minus(Duration.ofDays(6))).build();
        BookLoan returned = BookLoan.builder().book(book).student(ana)
                .borrowedAt(NOW.minus(Duration.ofDays(60))).dueAt(NOW.minus(Duration.ofDays(46)))
                .returnedAt(NOW.minus(Duration.ofDays(50))).build();
        when(loanRepository.findAllByOrderByBorrowedAtDesc()).thenReturn(List.of(late, returned));

        String csv = new LoanCsvExporter(loanRepository, Clock.fixed(NOW, ZoneOffset.UTC)).export();

        assertThat(csv.split("\r\n")).containsExactly(
                "book,student,borrowed_at,due_at,returned_at,overdue",
                "Late Book,\"Smith, Ana\",2026-02-28T10:00:00Z,2026-03-14T10:00:00Z,,true",
                "Late Book,\"Smith, Ana\",2026-01-19T10:00:00Z,2026-02-02T10:00:00Z,2026-01-29T10:00:00Z,false");
    }
}
