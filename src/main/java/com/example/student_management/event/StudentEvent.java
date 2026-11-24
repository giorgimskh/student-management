package com.example.student_management.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened to a student. {@code relatedId} is the book or course
 * involved, or {@code null} for events about the student alone.
 */
public record StudentEvent(
        StudentEventType type,
        UUID studentId,
        UUID relatedId,
        Instant occurredAt
) {
}
