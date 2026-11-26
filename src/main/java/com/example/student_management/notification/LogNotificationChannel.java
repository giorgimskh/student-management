package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Writes the message to the application log instead of keeping it. Handy for development. */
@Component
@ConditionalOnProperty(name = "app.notifications.channel", havingValue = "log")
public class LogNotificationChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(LogNotificationChannel.class);

    @Override
    public String name() {
        return "log";
    }

    @Override
    public void deliver(Student recipient, String subject, String body, Instant sentAt) {
        log.info("[NOTIFY] student={} subject=\"{}\" body=\"{}\"", recipient.getId(), subject, body);
    }
}
