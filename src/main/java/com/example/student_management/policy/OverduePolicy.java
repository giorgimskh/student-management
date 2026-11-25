package com.example.student_management.policy;

import com.example.student_management.domain.BookLoan;

import java.time.Instant;
import java.util.List;

/**
 * Strategy: what the library does about overdue books, beyond the BOOK_OVERDUE
 * event that is always published once per loan. Exactly one policy is active,
 * chosen with {@code app.loans.overdue-policy} ({@code notify} or {@code suspend}).
 */
public interface OverduePolicy {
    /** Short id shown in the API and UI, e.g. "notify". */
    String name();

    /** One sentence describing the policy for people, e.g. for the Books tab. */
    String description();

    /**
     * Called on every overdue check with every loan that is overdue right now
     * (not only the newly overdue ones), so a policy can act once a threshold
     * is passed. Must be safe to call repeatedly with the same loans.
     *
     * @return a human-readable line per action taken, empty if nothing was done
     */
    List<String> apply(List<BookLoan> overdueLoans, Instant now);
}
