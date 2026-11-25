package com.example.student_management.dto;

import com.example.student_management.domain.BookLoan;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        UUID studentId,
        String studentName,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        boolean overdue
) {
    /** {@code now} decides whether an open loan counts as overdue. */
    public static LoanResponse from(BookLoan loan, Instant now) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getStudent().getId(),
                loan.getStudent().getName(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                loan.isOverdue(now)
        );
    }
}
