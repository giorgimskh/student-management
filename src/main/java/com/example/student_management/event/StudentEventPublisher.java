package com.example.student_management.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject: keeps the list of observers and notifies each of them when a
 * student event happens. Every {@link StudentEventListener} bean is subscribed
 * automatically at startup; more can be added or removed at runtime.
 */
@Component
public class StudentEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(StudentEventPublisher.class);

    private final CopyOnWriteArrayList<StudentEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public StudentEventPublisher(List<StudentEventListener> listeners, Clock clock) {
        this.listeners.addAll(listeners);
        this.clock = clock;
    }

    public void subscribe(StudentEventListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void unsubscribe(StudentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(StudentEventType type, UUID studentId) {
        publish(type, studentId, null);
    }

    public void publish(StudentEventType type, UUID studentId, UUID relatedId) {
        publish(new StudentEvent(type, studentId, relatedId, Instant.now(clock)));
    }

    public void publish(StudentEvent event) {
        for (StudentEventListener listener : listeners) {
            try {
                listener.onStudentEvent(event);
            } catch (RuntimeException e) {
                // One failing observer must not break the request or the other observers.
                log.error("Listener {} failed to handle {}", listener.getClass().getSimpleName(), event, e);
            }
        }
    }
}
