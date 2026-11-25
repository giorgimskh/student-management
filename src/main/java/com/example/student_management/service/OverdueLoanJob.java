package com.example.student_management.service;

import com.example.student_management.policy.OverduePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * The overdue check, run on a schedule ({@code app.loans.overdue-check-cron}) or
 * by hand: reports loans that just became overdue, then lets the active
 * {@link OverduePolicy} act on everything that is overdue.
 */
@Component
public class OverdueLoanJob {
    private static final Logger log = LoggerFactory.getLogger(OverdueLoanJob.class);

    /** What one check did. */
    public record Result(int newlyOverdue, String policy, List<String> actions) {
    }

    private final LoanService loanService;
    private final OverduePolicy overduePolicy;
    private final Clock clock;

    public OverdueLoanJob(LoanService loanService, OverduePolicy overduePolicy, Clock clock) {
        this.loanService = loanService;
        this.overduePolicy = overduePolicy;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.loans.overdue-check-cron:0 0 * * * *}")
    public void run() {
        runCheck();
    }

    public Result runCheck() {
        int newlyOverdue = loanService.reportNewlyOverdueLoans();
        List<String> actions = overduePolicy.apply(loanService.overdueLoans(), Instant.now(clock));
        if (newlyOverdue > 0 || !actions.isEmpty()) {
            log.info("Overdue check: {} newly overdue, policy '{}' took {} action(s): {}",
                    newlyOverdue, overduePolicy.name(), actions.size(), actions);
        }
        return new Result(newlyOverdue, overduePolicy.name(), actions);
    }

    public OverduePolicy policy() {
        return overduePolicy;
    }
}
