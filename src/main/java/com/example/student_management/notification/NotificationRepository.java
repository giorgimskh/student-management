package com.example.student_management.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /** The newest 50, newest first. */
    List<Notification> findTop50ByStudentIdOrderByCreatedAtDesc(UUID studentId);

    long countByStudentIdAndReadAtIsNull(UUID studentId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.student.id = :studentId and n.readAt is null")
    int markAllRead(@Param("studentId") UUID studentId, @Param("readAt") Instant readAt);
}
