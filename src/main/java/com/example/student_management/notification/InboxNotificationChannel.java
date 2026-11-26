package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Saves the message in the student's in-app inbox ({@code notifications} table). The default channel. */
@Component
@ConditionalOnProperty(name = "app.notifications.channel", havingValue = "inbox", matchIfMissing = true)
public class InboxNotificationChannel implements NotificationChannel {
    private final NotificationRepository notificationRepository;

    public InboxNotificationChannel(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public String name() {
        return "inbox";
    }

    @Override
    public void deliver(Student recipient, String subject, String body, Instant sentAt) {
        notificationRepository.save(Notification.builder()
                .student(recipient)
                .subject(subject)
                .body(body)
                .createdAt(sentAt)
                .build());
    }
}
