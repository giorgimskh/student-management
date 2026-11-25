package com.example.student_management.policy;

import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentTransition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Suspends a student once any of their books is more than
 * {@code app.loans.suspend-after-days} overdue. Only ACTIVE students are
 * suspended; the State pattern decides that ({@link Student#getState()}).
 * A student reactivated while still holding the overdue book is suspended
 * again on the next check, so the book has to come back first.
 */
@Component
@ConditionalOnProperty(name = "app.loans.overdue-policy", havingValue = "suspend")
public class SuspendStudentPolicy implements OverduePolicy {
    private final StudentService studentService;
    private final Duration grace;

    public SuspendStudentPolicy(StudentService studentService,
                                @Value("${app.loans.suspend-after-days:7}") int suspendAfterDays) {
        if (suspendAfterDays < 0) {
            throw new IllegalArgumentException("app.loans.suspend-after-days must not be negative, was " + suspendAfterDays);
        }
        this.studentService = studentService;
        this.grace = Duration.ofDays(suspendAfterDays);
    }

    @Override
    public String name() {
        return "suspend";
    }

    @Override
    public String description() {
        return "Students are suspended when a book is more than " + grace.toDays() + " day(s) overdue.";
    }

    @Override
    public List<String> apply(List<BookLoan> overdueLoans, Instant now) {
        Set<UUID> handled = new LinkedHashSet<>(); // a student with two late books is suspended once
        List<String> actions = new ArrayList<>();
        for (BookLoan loan : overdueLoans) {
            Student student = loan.getStudent();
            boolean pastGrace = loan.getDueAt().plus(grace).isBefore(now);
            if (!pastGrace || !handled.add(student.getId())) {
                continue;
            }
            if (!student.getState().canBorrowBooks()) {
                continue; // already suspended, graduated or withdrawn
            }
            studentService.changeStatus(student.getId(), StudentTransition.SUSPEND);
            actions.add("Suspended " + student.getName() + ": \"" + loan.getBook().getTitle() + "\" is overdue");
        }
        return actions;
    }
}
