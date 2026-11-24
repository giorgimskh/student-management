package com.example.student_management.controller;

import com.example.student_management.event.StudentActivityLog;
import com.example.student_management.event.StudentEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ActivityController {
    private final StudentActivityLog activityLog;

    public ActivityController(StudentActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    @GetMapping("/api/activity")
    public ResponseEntity<List<StudentEvent>> getRecentActivity() {
        return ResponseEntity.ok(activityLog.recent());
    }

    @GetMapping("/api/students/{studentId}/activity")
    public ResponseEntity<List<StudentEvent>> getStudentActivity(@PathVariable UUID studentId) {
        return ResponseEntity.ok(activityLog.recentFor(studentId));
    }
}
