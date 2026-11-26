package com.example.student_management.notification;

import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reads and marks a student's inbox. Empty when the log channel is in use. */
@Service
public class NotificationInbox {
    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final Clock clock;

    public NotificationInbox(NotificationRepository notificationRepository, StudentRepository studentRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.clock = clock;
    }

    /** The newest 50, newest first. */
    @Transactional(readOnly = true)
    public List<Notification> recent(UUID studentId) {
        requireStudent(studentId);
        return notificationRepository.findTop50ByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID studentId) {
        requireStudent(studentId);
        return notificationRepository.countByStudentIdAndReadAtIsNull(studentId);
    }

    /** @return how many were unread */
    @Transactional
    public int markAllRead(UUID studentId) {
        requireStudent(studentId);
        return notificationRepository.markAllRead(studentId, Instant.now(clock));
    }

    private void requireStudent(UUID studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with id: " + studentId);
        }
    }
}
