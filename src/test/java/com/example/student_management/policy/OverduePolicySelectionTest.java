package com.example.student_management.policy;

import com.example.student_management.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The Strategy is chosen by configuration: exactly one OverduePolicy bean exists. */
class OverduePolicySelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(StudentService.class, () -> mock(StudentService.class))
            .withUserConfiguration(NotifyOnlyPolicy.class, SuspendStudentPolicy.class);

    @Test
    void defaultsToNotify() {
        runner.run(context -> assertThat(context).hasSingleBean(OverduePolicy.class)
                .getBean(OverduePolicy.class).isInstanceOf(NotifyOnlyPolicy.class));
    }

    @Test
    void suspendWhenConfigured_withItsOwnThreshold() {
        runner.withPropertyValues("app.loans.overdue-policy=suspend", "app.loans.suspend-after-days=3")
                .run(context -> {
                    assertThat(context).hasSingleBean(OverduePolicy.class);
                    OverduePolicy policy = context.getBean(OverduePolicy.class);
                    assertThat(policy).isInstanceOf(SuspendStudentPolicy.class);
                    assertThat(policy.description()).contains("3 day(s)");
                });
    }

    @Test
    void unknownPolicyName_leavesNoPolicy_soTheAppFailsToStart() {
        runner.withPropertyValues("app.loans.overdue-policy=email")
                .run(context -> assertThat(context).doesNotHaveBean(OverduePolicy.class));
    }
}
