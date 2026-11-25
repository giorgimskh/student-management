package com.example.student_management.repository;

import com.example.student_management.domain.BookLoan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookLoanRepository extends JpaRepository<BookLoan, UUID> {
    @EntityGraph(attributePaths = {"book", "student"})
    List<BookLoan> findByStudentIdOrderByBorrowedAtDesc(UUID studentId);

    @EntityGraph(attributePaths = {"book", "student"})
    List<BookLoan> findByReturnedAtIsNullOrderByDueAtAsc();

    @EntityGraph(attributePaths = {"book", "student"})
    List<BookLoan> findByReturnedAtIsNullAndDueAtBeforeOrderByDueAtAsc(Instant now);

    /** Overdue loans the overdue check has not reported yet. */
    @EntityGraph(attributePaths = {"book", "student"})
    List<BookLoan> findByReturnedAtIsNullAndDueAtBeforeAndOverdueNotifiedAtIsNull(Instant now);

    Optional<BookLoan> findFirstByBookIdAndReturnedAtIsNull(UUID bookId);

    /** Every loan, newest first (CSV export). */
    @EntityGraph(attributePaths = {"book", "student"})
    List<BookLoan> findAllByOrderByBorrowedAtDesc();
}
