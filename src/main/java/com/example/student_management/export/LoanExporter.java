package com.example.student_management.export;

import com.example.student_management.domain.BookLoan;
import com.example.student_management.repository.BookLoanRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** The full loan history, newest first, including whether each open loan is overdue. */
@Component
public class LoanExporter extends TableExporter<BookLoan> {
    private final BookLoanRepository loanRepository;
    private final Clock clock;
    private Instant now;

    public LoanExporter(BookLoanRepository loanRepository, Clock clock) {
        this.loanRepository = loanRepository;
        this.clock = clock;
    }

    @Override
    public String type() {
        return "loans";
    }

    /** One "now" for the whole file, so every row is judged overdue against the same moment. */
    @Override
    protected void prepare() {
        now = Instant.now(clock);
    }

    @Override
    protected List<String> headers() {
        return List.of("book", "student", "borrowed_at", "due_at", "returned_at", "overdue");
    }

    @Override
    protected List<BookLoan> records() {
        return loanRepository.findAllByOrderByBorrowedAtDesc();
    }

    @Override
    protected List<?> toRow(BookLoan loan) {
        return Arrays.asList(loan.getBook().getTitle(), loan.getStudent().getName(),
                loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt(), loan.isOverdue(now));
    }
}
