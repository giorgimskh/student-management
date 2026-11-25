package com.example.student_management.policy;

import com.example.student_management.domain.BookLoan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** The default: overdue loans are reported (BOOK_OVERDUE event) and nothing else happens. */
@Component
@ConditionalOnProperty(name = "app.loans.overdue-policy", havingValue = "notify", matchIfMissing = true)
public class NotifyOnlyPolicy implements OverduePolicy {
    @Override
    public String name() {
        return "notify";
    }

    @Override
    public String description() {
        return "Overdue books are reported in the activity feed.";
    }

    @Override
    public List<String> apply(List<BookLoan> overdueLoans, Instant now) {
        return List.of();
    }
}
