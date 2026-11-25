package com.example.student_management.service;

import com.example.student_management.domain.BookLoan;
import com.example.student_management.policy.OverduePolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueLoanJobTest {
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");

    @Mock
    private LoanService loanService;

    @Mock
    private OverduePolicy policy;

    @Test
    void reportsFirst_thenHandsAllOverdueLoansToThePolicy() {
        List<BookLoan> overdue = List.of(new BookLoan());
        when(loanService.reportNewlyOverdueLoans()).thenReturn(2);
        when(loanService.overdueLoans()).thenReturn(overdue);
        when(policy.apply(overdue, NOW)).thenReturn(List.of("Suspended Ana"));
        when(policy.name()).thenReturn("suspend");
        OverdueLoanJob job = new OverdueLoanJob(loanService, policy, Clock.fixed(NOW, ZoneOffset.UTC));

        OverdueLoanJob.Result result = job.runCheck();

        assertThat(result).isEqualTo(new OverdueLoanJob.Result(2, "suspend", List.of("Suspended Ana")));
        InOrder order = inOrder(loanService, policy);
        order.verify(loanService).reportNewlyOverdueLoans();
        order.verify(policy).apply(overdue, NOW);
    }
}
