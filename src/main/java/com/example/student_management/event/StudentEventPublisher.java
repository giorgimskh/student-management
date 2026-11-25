package com.example.student_management.event;

import java.util.UUID;

/**
 * Subject of the observer pattern: notifies {@link StudentEventListener}s when
 * something happens to a student. Callers depend on this interface, so the
 * delivery mechanism (in-process, async, message broker, ...) can be swapped.
 */
public interface StudentEventPublisher {
    void subscribe(StudentEventListener listener);

    void unsubscribe(StudentEventListener listener);

    /** Publishes an event that concerns only the student. */
    default void publish(StudentEventType type, UUID studentId) {
        publish(type, studentId, null);
    }

    /** Publishes an event about the student and a related book or course; timestamped by the implementation. */
    void publish(StudentEventType type, UUID studentId, UUID relatedId);

    void publish(StudentEvent event);
}
