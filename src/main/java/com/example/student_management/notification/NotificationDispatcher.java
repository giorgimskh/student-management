package com.example.student_management.notification;

import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventListener;
import com.example.student_management.repository.StudentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Observer that sends the matching {@link StudentNotification}s for each
 * student event. It works only with the abstraction: which channel delivers
 * is the notifications' business. Events with no matching notification, and
 * events about a student who no longer exists, send nothing.
 */
@Component
public class NotificationDispatcher implements StudentEventListener {
    private final List<StudentNotification> notifications;
    private final StudentRepository studentRepository;

    public NotificationDispatcher(List<StudentNotification> notifications, StudentRepository studentRepository) {
        this.notifications = List.copyOf(notifications);
        this.studentRepository = studentRepository;
    }

    @Override
    public void onStudentEvent(StudentEvent event) {
        List<StudentNotification> matching = notifications.stream()
                .filter(n -> n.triggers().contains(event.type()))
                .toList();
        if (matching.isEmpty()) {
            return;
        }
        studentRepository.findById(event.studentId())
                .ifPresent(student -> matching.forEach(n -> n.send(student, event)));
    }
}
