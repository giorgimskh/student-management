package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;

import java.util.List;
import java.util.UUID;

/** Due dates and history for borrowed books. */
public interface LoanService {
    /** Starts a loan due after the configured loan period. */
    BookLoan openLoan(Book book, Student student);

    /** Marks the book's open loan as returned. Does nothing if it has none. */
    void closeLoan(UUID bookId);

    /** All loans of one student, newest first. */
    List<BookLoan> loansForStudent(UUID studentId);

    /** Books currently borrowed, soonest due first. */
    List<BookLoan> activeLoans();

    /** Borrowed books past their due date, most overdue first. */
    List<BookLoan> overdueLoans();

    /**
     * Publishes a BOOK_OVERDUE event for each loan that has become overdue since
     * the last check, and remembers it so it is reported only once.
     *
     * @return how many loans were newly reported
     */
    int reportNewlyOverdueLoans();
}
