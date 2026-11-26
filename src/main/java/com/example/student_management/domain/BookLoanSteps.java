package com.example.student_management.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Step builder for a {@link BookLoan}, started with {@link BookLoan#lend(Book)}:
 *
 * <pre>{@code
 * BookLoan loan = BookLoan.lend(book).to(student).borrowedAt(now).dueAfter(Duration.ofDays(14)).build();
 * }</pre>
 *
 * Each step returns an interface that offers only the next step, so a loan
 * without a student, borrow time or due date does not compile, and the parts
 * cannot be given in the wrong order. Values are checked as they are given:
 * nothing may be null, the due date must be after the borrow time, and a
 * return cannot come before the borrow.
 *
 * <p>Lombok's {@code BookLoan.builder()} is still there for code that needs a
 * partial loan, such as tests; new loans in the application use this one.
 */
public final class BookLoanSteps {
    private BookLoanSteps() {
    }

    static StudentStep start(Book book) {
        return new Steps(Objects.requireNonNull(book, "book"));
    }

    /** Who borrows the book. */
    public interface StudentStep {
        BorrowedAtStep to(Student student);
    }

    /** When the book was borrowed. */
    public interface BorrowedAtStep {
        DueStep borrowedAt(Instant borrowedAt);
    }

    /** When it must come back, as a date or as a period after borrowing. */
    public interface DueStep {
        /** @throws IllegalArgumentException if not after the borrow time */
        OptionalStep dueAt(Instant dueAt);

        /** @throws IllegalArgumentException if the period is zero or negative */
        OptionalStep dueAfter(Duration loanPeriod);
    }

    /** Everything required is set: optional parts, then {@link #build()}. */
    public interface OptionalStep {
        /** For a loan that is already over, e.g. when importing history. */
        OptionalStep returnedAt(Instant returnedAt);

        BookLoan build();
    }

    private static final class Steps implements StudentStep, BorrowedAtStep, DueStep, OptionalStep {
        private final Book book;
        private Student student;
        private Instant borrowedAt;
        private Instant dueAt;
        private Instant returnedAt;

        private Steps(Book book) {
            this.book = book;
        }

        @Override
        public BorrowedAtStep to(Student student) {
            this.student = Objects.requireNonNull(student, "student");
            return this;
        }

        @Override
        public DueStep borrowedAt(Instant borrowedAt) {
            this.borrowedAt = Objects.requireNonNull(borrowedAt, "borrowedAt");
            return this;
        }

        @Override
        public OptionalStep dueAt(Instant dueAt) {
            Objects.requireNonNull(dueAt, "dueAt");
            if (!dueAt.isAfter(borrowedAt)) {
                throw new IllegalArgumentException("Due date " + dueAt + " must be after the borrow time " + borrowedAt);
            }
            this.dueAt = dueAt;
            return this;
        }

        @Override
        public OptionalStep dueAfter(Duration loanPeriod) {
            Objects.requireNonNull(loanPeriod, "loanPeriod");
            if (loanPeriod.isZero() || loanPeriod.isNegative()) {
                throw new IllegalArgumentException("Loan period must be positive, was " + loanPeriod);
            }
            return dueAt(borrowedAt.plus(loanPeriod));
        }

        @Override
        public OptionalStep returnedAt(Instant returnedAt) {
            Objects.requireNonNull(returnedAt, "returnedAt");
            if (returnedAt.isBefore(borrowedAt)) {
                throw new IllegalArgumentException("Return time " + returnedAt + " is before the borrow time " + borrowedAt);
            }
            this.returnedAt = returnedAt;
            return this;
        }

        /** A new loan each call, so the steps can be reused for several identical loans. */
        @Override
        public BookLoan build() {
            return BookLoan.builder()
                    .book(book)
                    .student(student)
                    .borrowedAt(borrowedAt)
                    .dueAt(dueAt)
                    .returnedAt(returnedAt)
                    .build();
        }
    }
}
