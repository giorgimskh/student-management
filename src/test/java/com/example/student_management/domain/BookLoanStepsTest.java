package com.example.student_management.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link BookLoan#lend(Book)}: the step builder. Leaving out a required step is a compile error, so not tested here. */
class BookLoanStepsTest {
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private final Book book = Book.builder().id(UUID.randomUUID()).title("Clean Code").build();
    private final Student student = Student.builder().id(UUID.randomUUID()).name("Ana").build();

    @Test
    void buildsAnOpenLoan_withADueDateFromThePeriod() {
        BookLoan loan = BookLoan.lend(book).to(student).borrowedAt(NOW).dueAfter(Duration.ofDays(14)).build();

        assertThat(loan.getBook()).isSameAs(book);
        assertThat(loan.getStudent()).isSameAs(student);
        assertThat(loan.getBorrowedAt()).isEqualTo(NOW);
        assertThat(loan.getDueAt()).isEqualTo(Instant.parse("2026-03-15T10:00:00Z"));
        assertThat(loan.getId()).isNull();
        assertThat(loan.isOpen()).isTrue();
    }

    @Test
    void dueDate_canBeGivenDirectly_andAReturnAdded() {
        Instant due = NOW.plus(Duration.ofDays(7));

        BookLoan loan = BookLoan.lend(book).to(student).borrowedAt(NOW).dueAt(due).returnedAt(NOW.plus(Duration.ofDays(2))).build();

        assertThat(loan.getDueAt()).isEqualTo(due);
        assertThat(loan.isOpen()).isFalse();
    }

    @Test
    void eachBuild_givesANewLoan() {
        BookLoanSteps.OptionalStep steps = BookLoan.lend(book).to(student).borrowedAt(NOW).dueAfter(Duration.ofDays(1));

        assertThat(steps.build()).isNotSameAs(steps.build());
    }

    @Test
    void dueDate_mustBeAfterTheBorrowTime() {
        BookLoanSteps.DueStep due = BookLoan.lend(book).to(student).borrowedAt(NOW);

        assertThatThrownBy(() -> due.dueAt(NOW)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must be after");
        assertThatThrownBy(() -> due.dueAt(NOW.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> due.dueAfter(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> due.dueAfter(Duration.ofDays(-1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnTime_cannotBeBeforeTheBorrowTime() {
        BookLoanSteps.OptionalStep steps = BookLoan.lend(book).to(student).borrowedAt(NOW).dueAfter(Duration.ofDays(1));

        assertThatThrownBy(() -> steps.returnedAt(NOW.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before the borrow time");
    }

    @Test
    void nullParts_areRejectedAtTheStepThatTakesThem() {
        assertThatThrownBy(() -> BookLoan.lend(null)).isInstanceOf(NullPointerException.class).hasMessage("book");
        assertThatThrownBy(() -> BookLoan.lend(book).to(null)).isInstanceOf(NullPointerException.class).hasMessage("student");
        assertThatThrownBy(() -> BookLoan.lend(book).to(student).borrowedAt(null))
                .isInstanceOf(NullPointerException.class).hasMessage("borrowedAt");
        assertThatThrownBy(() -> BookLoan.lend(book).to(student).borrowedAt(NOW).dueAt(null))
                .isInstanceOf(NullPointerException.class).hasMessage("dueAt");
    }
}
