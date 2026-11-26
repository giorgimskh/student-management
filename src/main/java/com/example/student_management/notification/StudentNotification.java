package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;

import java.util.Objects;
import java.util.Set;

/**
 * Bridge, abstraction side: a kind of message sent to a student when
 * something happens to them. Subclasses decide which events they are for and
 * what to say; how the message is delivered is left to the
 * {@link NotificationChannel} this notification holds. The two sides vary
 * independently: N kinds of message and M channels need N + M classes, not N × M.
 */
public abstract class StudentNotification {
    private final NotificationChannel channel;

    protected StudentNotification(NotificationChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    /** The events this notification is sent for. */
    public abstract Set<StudentEventType> triggers();

    /** Words the message and hands it to the channel. */
    public final void send(Student student, StudentEvent event) {
        channel.deliver(student, subject(student, event), body(student, event), event.occurredAt());
    }

    protected abstract String subject(Student student, StudentEvent event);

    protected abstract String body(Student student, StudentEvent event);

    public NotificationChannel channel() {
        return channel;
    }
}
