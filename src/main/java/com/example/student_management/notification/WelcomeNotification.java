package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Sent once, when the student is added. */
@Component
public class WelcomeNotification extends StudentNotification {
    public WelcomeNotification(NotificationChannel channel) {
        super(channel);
    }

    @Override
    public Set<StudentEventType> triggers() {
        return Set.of(StudentEventType.STUDENT_CREATED);
    }

    @Override
    protected String subject(Student student, StudentEvent event) {
        return "Welcome, " + student.getName();
    }

    @Override
    protected String body(Student student, StudentEvent event) {
        return "Your account is ready. You can now borrow books from the library and enroll in courses.";
    }
}
