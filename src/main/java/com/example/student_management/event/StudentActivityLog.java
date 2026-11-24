package com.example.student_management.event;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Observer that keeps the most recent student events in memory so they can be
 * served by the activity endpoint. Oldest events are dropped past {@link #CAPACITY}.
 */
@Component
public class StudentActivityLog implements StudentEventListener {
    static final int CAPACITY = 100;

    private final Deque<StudentEvent> events = new ArrayDeque<>();

    @Override
    public synchronized void onStudentEvent(StudentEvent event) {
        if (events.size() == CAPACITY) {
            events.removeLast();
        }
        events.addFirst(event);
    }

    /** Most recent first. */
    public synchronized List<StudentEvent> recent() {
        return new ArrayList<>(events);
    }

    /** Most recent first. */
    public synchronized List<StudentEvent> recentFor(UUID studentId) {
        return events.stream().filter(e -> e.studentId().equals(studentId)).toList();
    }
}
