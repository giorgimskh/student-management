package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEventPublisher;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {
    private static final Instant NOW = Instant.parse("2026-03-01T09:00:00Z");

    @Mock
    private BookLoanRepository loanRepository;

    @Mock
    private StudentEventPublisher eventPublisher;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanServiceImpl(loanRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC), 14);
    }

    private static Student student() {
        return Student.builder().id(UUID.randomUUID()).name("Nino").build();
    }

    private static Book book() {
        return Book.builder().id(UUID.randomUUID()).title("Clean Code").build();
    }

    @Test
    void openLoan_startsNow_andIsDueAfterTheLoanPeriod() {
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Book book = book();
        Student student = student();

        BookLoan loan = loanService.openLoan(book, student);

        assertThat(loan.getBook()).isSameAs(book);
        assertThat(loan.getStudent()).isSameAs(student);
        assertThat(loan.getBorrowedAt()).isEqualTo(NOW);
        assertThat(loan.getDueAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
        assertThat(loan.isOpen()).isTrue();
    }

    @Test
    void closeLoan_setsReturnedAt() {
        UUID bookId = UUID.randomUUID();
        BookLoan open = BookLoan.builder().borrowedAt(NOW.minus(Duration.ofDays(3))).dueAt(NOW.plus(Duration.ofDays(11))).build();
        when(loanRepository.findFirstByBookIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.of(open));

        loanService.closeLoan(bookId);

        assertThat(open.getReturnedAt()).isEqualTo(NOW);
        verify(loanRepository).save(open);
    }

    @Test
    void closeLoan_withoutOpenLoan_doesNothing() {
        UUID bookId = UUID.randomUUID();
        when(loanRepository.findFirstByBookIdAndReturnedAtIsNull(bookId)).thenReturn(Optional.empty());

        loanService.closeLoan(bookId);

        verify(loanRepository, never()).save(any());
    }

    @Test
    void overdueLoans_usesTheCurrentTime() {
        loanService.overdueLoans();

        verify(loanRepository).findByReturnedAtIsNullAndDueAtBeforeOrderByDueAtAsc(NOW);
    }

    @Test
    void reportNewlyOverdueLoans_marksEachLoanAndPublishesOneEventPerLoan() {
        Student student = student();
        Book book = book();
        BookLoan loan = BookLoan.builder().student(student).book(book)
                .borrowedAt(NOW.minus(Duration.ofDays(20))).dueAt(NOW.minus(Duration.ofDays(6))).build();
        when(loanRepository.findByReturnedAtIsNullAndDueAtBeforeAndOverdueNotifiedAtIsNull(NOW)).thenReturn(List.of(loan));

        int reported = loanService.reportNewlyOverdueLoans();

        assertThat(reported).isEqualTo(1);
        assertThat(loan.getOverdueNotifiedAt()).isEqualTo(NOW);
        verify(loanRepository).save(loan);
        verify(eventPublisher).publish(StudentEventType.BOOK_OVERDUE, student.getId(), book.getId());
    }

    @Test
    void reportNewlyOverdueLoans_withNothingNew_publishesNothing() {
        when(loanRepository.findByReturnedAtIsNullAndDueAtBeforeAndOverdueNotifiedAtIsNull(NOW)).thenReturn(List.of());

        assertThat(loanService.reportNewlyOverdueLoans()).isZero();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void loanPeriodMustBePositive() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        assertThatThrownBy(() -> new LoanServiceImpl(loanRepository, eventPublisher, clock, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.loans.period-days");
    }

    @Test
    void isOverdue_onlyForOpenLoansPastTheirDueDate() {
        BookLoan dueTomorrow = BookLoan.builder().dueAt(NOW.plus(Duration.ofDays(1))).build();
        BookLoan dueYesterday = BookLoan.builder().dueAt(NOW.minus(Duration.ofDays(1))).build();
        BookLoan returnedLate = BookLoan.builder().dueAt(NOW.minus(Duration.ofDays(1))).returnedAt(NOW).build();

        assertThat(dueTomorrow.isOverdue(NOW)).isFalse();
        assertThat(dueYesterday.isOverdue(NOW)).isTrue();
        assertThat(returnedLate.isOverdue(NOW)).isFalse();
    }
}
