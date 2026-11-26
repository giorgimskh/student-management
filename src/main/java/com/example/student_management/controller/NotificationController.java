package com.example.student_management.controller;

import com.example.student_management.dto.NotificationResponse;
import com.example.student_management.notification.NotificationInbox;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A student's in-app inbox, filled by the inbox notification channel. */
@RestController
@RequestMapping("/api/students/{studentId}/notifications")
public class NotificationController {
    private final NotificationInbox inbox;

    public NotificationController(NotificationInbox inbox) {
        this.inbox = inbox;
    }

    /** The newest 50, newest first. */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@PathVariable UUID studentId) {
        return ResponseEntity.ok(inbox.recent(studentId).stream().map(NotificationResponse::from).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@PathVariable UUID studentId) {
        return ResponseEntity.ok(Map.of("unread", inbox.unreadCount(studentId)));
    }

    /** Marks every unread message as read; returns how many there were. */
    @PostMapping("/read")
    public ResponseEntity<Map<String, Integer>> markAllRead(@PathVariable UUID studentId) {
        return ResponseEntity.ok(Map.of("marked", inbox.markAllRead(studentId)));
    }
}
