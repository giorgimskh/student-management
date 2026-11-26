package com.example.student_management.controller;

import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.notification.Notification;
import com.example.student_management.notification.NotificationInbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationInbox inbox;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void list_returnsTheMessages() throws Exception {
        UUID id = UUID.randomUUID();
        when(inbox.recent(studentId)).thenReturn(List.of(Notification.builder().id(id).subject("Welcome, Ana")
                .body("Your account is ready.").createdAt(Instant.parse("2026-03-01T10:00:00Z")).build()));

        mockMvc.perform(get("/api/students/{id}/notifications", studentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].subject").value("Welcome, Ana"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-03-01T10:00:00Z"))
                .andExpect(jsonPath("$[0].readAt").doesNotExist());
    }

    @Test
    void unreadCount_andMarkAllRead() throws Exception {
        when(inbox.unreadCount(studentId)).thenReturn(3L);
        when(inbox.markAllRead(studentId)).thenReturn(3);

        mockMvc.perform(get("/api/students/{id}/notifications/unread-count", studentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unread").value(3));
        mockMvc.perform(post("/api/students/{id}/notifications/read", studentId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.marked").value(3));
    }

    @Test
    void unknownStudent_is404() throws Exception {
        when(inbox.recent(studentId)).thenThrow(new ResourceNotFoundException("Student not found with id: " + studentId));

        mockMvc.perform(get("/api/students/{id}/notifications", studentId)).andExpect(status().isNotFound());
    }
}
