package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/** Sent when the student's status changes, including when an undo puts it back. */
@Component
public class StatusChangeNotice extends StudentNotification {
    public StatusChangeNotice(NotificationChannel channel) {
        super(channel);
    }

    @Override
    public Set<StudentEventType> triggers() {
        return Set.of(StudentEventType.STUDENT_SUSPENDED, StudentEventType.STUDENT_REACTIVATED,
                StudentEventType.STUDENT_GRADUATED, StudentEventType.STUDENT_WITHDRAWN,
                StudentEventType.STUDENT_STATUS_RESTORED);
    }

    @Override
    protected String subject(Student student, StudentEvent event) {
        return switch (event.type()) {
            case STUDENT_SUSPENDED -> "Your account is suspended";
            case STUDENT_REACTIVATED -> "Your account is active again";
            case STUDENT_GRADUATED -> "Congratulations on graduating";
            case STUDENT_WITHDRAWN -> "You have withdrawn";
            default -> "Your status was changed back";
        };
    }

    @Override
    protected String body(Student student, StudentEvent event) {
        return switch (event.type()) {
            case STUDENT_SUSPENDED -> "You cannot borrow books, enroll in courses or edit your profile until you are reactivated.";
            case STUDENT_REACTIVATED -> "You can borrow books and enroll in courses again.";
            case STUDENT_GRADUATED -> "Well done, " + student.getName() + "! Your record is kept.";
            case STUDENT_WITHDRAWN -> "Your record is kept, but it can no longer be changed.";
            default -> "A recent status change was undone. Your status is "
                    + (student.getStatus() == null ? "unchanged" : student.getStatus().name().toLowerCase(Locale.ROOT)) + ".";
        };
    }
}
