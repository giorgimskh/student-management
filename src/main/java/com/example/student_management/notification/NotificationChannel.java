package com.example.student_management.notification;

import com.example.student_management.domain.Student;

import java.time.Instant;

/**
 * Bridge, implementation side: how a message reaches a student. A channel
 * knows nothing about what the message is for; it only delivers a subject and
 * a body. The other side, {@link StudentNotification}, decides the wording and
 * holds a channel, so new kinds of message and new channels can be added
 * independently, without a class for every combination.
 *
 * <p>Exactly one channel is active, chosen with {@code app.notifications.channel}
 * ({@code inbox}, the default, or {@code log}).
 */
public interface NotificationChannel {
    /** Short id, e.g. "inbox". */
    String name();

    void deliver(Student recipient, String subject, String body, Instant sentAt);
}
