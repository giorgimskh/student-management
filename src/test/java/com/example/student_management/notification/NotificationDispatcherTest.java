package com.example.student_management.notification;

import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {
    private final StudentNotificationTest.RecordingChannel channel = new StudentNotificationTest.RecordingChannel();
    private final StudentRepository studentRepository = mock(StudentRepository.class);
    private final NotificationDispatcher dispatcher = new NotificationDispatcher(
            List.of(new WelcomeNotification(channel), new StatusChangeNotice(channel)), studentRepository);

    private final Student ana = Student.builder().id(UUID.randomUUID()).name("Ana").build();

    private StudentEvent event(StudentEventType type) {
        return new StudentEvent(type, ana.getId(), null, Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void sendsTheNotificationForTheEvent_toTheStudentLoadedById() {
        when(studentRepository.findById(ana.getId())).thenReturn(Optional.of(ana));

        dispatcher.onStudentEvent(event(StudentEventType.STUDENT_SUSPENDED));

        assertThat(channel.deliveries).singleElement().satisfies(d -> {
            assertThat(d.recipient()).isSameAs(ana);
            assertThat(d.subject()).isEqualTo("Your account is suspended");
        });
    }

    @Test
    void eventWithoutANotification_doesNotEvenLoadTheStudent() {
        dispatcher.onStudentEvent(event(StudentEventType.STUDENT_UPDATED));

        assertThat(channel.deliveries).isEmpty();
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void studentThatNoLongerExists_getsNothing() {
        when(studentRepository.findById(ana.getId())).thenReturn(Optional.empty());

        dispatcher.onStudentEvent(event(StudentEventType.STUDENT_CREATED));

        assertThat(channel.deliveries).isEmpty();
    }
}
