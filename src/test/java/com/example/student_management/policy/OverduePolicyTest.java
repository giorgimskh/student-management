package com.example.student_management.policy;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverduePolicyTest {
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");

    @Mock
    private StudentService studentService;

    private static Student student(String name, StudentStatus status) {
        return Student.builder().id(UUID.randomUUID()).name(name).status(status).build();
    }

    /** A loan that is {@code daysOverdue} days past its due date. */
    private static BookLoan overdue(Student student, String title, int daysOverdue) {
        Instant due = NOW.minus(Duration.ofDays(daysOverdue));
        return BookLoan.builder().student(student).book(Book.builder().id(UUID.randomUUID()).title(title).build())
                .borrowedAt(due.minus(Duration.ofDays(14))).dueAt(due).build();
    }

    @Test
    void notifyOnly_takesNoAction() {
        NotifyOnlyPolicy policy = new NotifyOnlyPolicy();

        assertThat(policy.apply(List.of(overdue(student("Ana", StudentStatus.ACTIVE), "Book", 30)), NOW)).isEmpty();
        assertThat(policy.name()).isEqualTo("notify");
    }

    @Test
    void suspend_suspendsActiveStudentsPastTheGracePeriod() {
        Student ana = student("Ana", StudentStatus.ACTIVE);
        SuspendStudentPolicy policy = new SuspendStudentPolicy(studentService, 7);

        List<String> actions = policy.apply(List.of(overdue(ana, "Clean Code", 8)), NOW);

        verify(studentService).changeStatus(ana.getId(), StudentTransition.SUSPEND);
        assertThat(actions).containsExactly("Suspended Ana: \"Clean Code\" is overdue");
    }

    @Test
    void suspend_leavesStudentsWithinTheGracePeriodAlone() {
        SuspendStudentPolicy policy = new SuspendStudentPolicy(studentService, 7);

        assertThat(policy.apply(List.of(overdue(student("Ana", StudentStatus.ACTIVE), "Book", 7)), NOW)).isEmpty();
        verifyNoInteractions(studentService);
    }

    @Test
    void suspend_skipsStudentsWhoAreNotActive() {
        SuspendStudentPolicy policy = new SuspendStudentPolicy(studentService, 0);
        List<BookLoan> loans = List.of(
                overdue(student("Sus", StudentStatus.SUSPENDED), "A", 10),
                overdue(student("Grad", StudentStatus.GRADUATED), "B", 10),
                overdue(student("Gone", StudentStatus.WITHDRAWN), "C", 10));

        assertThat(policy.apply(loans, NOW)).isEmpty();
        verifyNoInteractions(studentService);
    }

    @Test
    void suspend_actsOncePerStudentEvenWithSeveralLateBooks() {
        Student ana = student("Ana", StudentStatus.ACTIVE);
        SuspendStudentPolicy policy = new SuspendStudentPolicy(studentService, 1);

        List<String> actions = policy.apply(List.of(overdue(ana, "First", 10), overdue(ana, "Second", 5)), NOW);

        verify(studentService, times(1)).changeStatus(any(), any());
        assertThat(actions).hasSize(1);
    }

    @Test
    void suspend_describesItsThreshold_andRejectsNegativeDays() {
        assertThat(new SuspendStudentPolicy(studentService, 3).description())
                .isEqualTo("Students are suspended when a book is more than 3 day(s) overdue.");
        assertThatThrownBy(() -> new SuspendStudentPolicy(studentService, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
