package com.example.student_management.event;

/**
 * Observer: implement this and declare it as a Spring bean to be notified
 * of every {@link StudentEvent} published by {@link StudentEventPublisher}.
 */
public interface StudentEventListener {
    void onStudentEvent(StudentEvent event);
}
