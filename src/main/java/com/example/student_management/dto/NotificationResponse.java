package com.example.student_management.dto;

import com.example.student_management.notification.Notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String subject, String body, Instant createdAt, Instant readAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getSubject(), notification.getBody(),
                notification.getCreatedAt(), notification.getReadAt());
    }
}
