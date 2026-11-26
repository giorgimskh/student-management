package com.example.student_management.service.impl;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEventPublisher;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.service.LoanService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LoanServiceImpl implements LoanService {
    private final BookLoanRepository loanRepository;
    private final StudentEventPublisher eventPublisher;
    private final Clock clock;
    private final Duration loanPeriod;

    public LoanServiceImpl(BookLoanRepository loanRepository,
                           StudentEventPublisher eventPublisher,
                           Clock clock,
                           @Value("${app.loans.period-days:14}") int periodDays) {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("app.loans.period-days must be positive, was " + periodDays);
        }
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.loanPeriod = Duration.ofDays(periodDays);
    }

    @Override
    public BookLoan openLoan(Book book, Student student) {
        Instant now = Instant.now(clock);
        return loanRepository.save(BookLoan.lend(book).to(student).borrowedAt(now).dueAfter(loanPeriod).build());
    }

    @Override
    public void closeLoan(UUID bookId) {
        loanRepository.findFirstByBookIdAndReturnedAtIsNull(bookId).ifPresent(loan -> {
            loan.setReturnedAt(Instant.now(clock));
            loanRepository.save(loan);
        });
    }

    @Override
    public List<BookLoan> loansForStudent(UUID studentId) {
        return loanRepository.findByStudentIdOrderByBorrowedAtDesc(studentId);
    }

    @Override
    public List<BookLoan> activeLoans() {
        return loanRepository.findByReturnedAtIsNullOrderByDueAtAsc();
    }

    @Override
    public List<BookLoan> overdueLoans() {
        return loanRepository.findByReturnedAtIsNullAndDueAtBeforeOrderByDueAtAsc(Instant.now(clock));
    }

    @Override
    @Transactional
    public int reportNewlyOverdueLoans() {
        Instant now = Instant.now(clock);
        List<BookLoan> newlyOverdue = loanRepository.findByReturnedAtIsNullAndDueAtBeforeAndOverdueNotifiedAtIsNull(now);
        for (BookLoan loan : newlyOverdue) {
            loan.setOverdueNotifiedAt(now);
            loanRepository.save(loan);
            eventPublisher.publish(StudentEventType.BOOK_OVERDUE, loan.getStudent().getId(), loan.getBook().getId());
        }
        return newlyOverdue.size();
    }
}
