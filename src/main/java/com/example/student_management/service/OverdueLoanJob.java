package com.example.student_management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically reports loans that have just become overdue (see {@code app.loans.overdue-check-cron}). */
@Component
public class OverdueLoanJob {
    private static final Logger log = LoggerFactory.getLogger(OverdueLoanJob.class);
    private final LoanService loanService;

    public OverdueLoanJob(LoanService loanService) {
        this.loanService = loanService;
    }

    @Scheduled(cron = "${app.loans.overdue-check-cron:0 0 * * * *}")
    public void run() {
        int reported = loanService.reportNewlyOverdueLoans();
        if (reported > 0) {
            log.info("Overdue check: {} loan(s) became overdue", reported);
        }
    }
}
