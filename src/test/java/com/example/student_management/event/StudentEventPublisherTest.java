package com.example.student_management.event;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentEventPublisherTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void publish_notifiesEveryRegisteredListener() {
        List<StudentEvent> first = new ArrayList<>();
        List<StudentEvent> second = new ArrayList<>();
        StudentEventPublisher publisher = new InMemoryStudentEventPublisher(List.of(first::add, second::add), clock);

        UUID studentId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        publisher.publish(StudentEventType.COURSE_ENROLLED, studentId, courseId);

        StudentEvent expected = new StudentEvent(StudentEventType.COURSE_ENROLLED, studentId, courseId, clock.instant());
        assertThat(first).containsExactly(expected);
        assertThat(second).containsExactly(expected);
    }

    @Test
    void unsubscribedListener_isNoLongerNotified() {
        List<StudentEvent> received = new ArrayList<>();
        StudentEventListener listener = received::add;
        StudentEventPublisher publisher = new InMemoryStudentEventPublisher(List.of(), clock);

        publisher.subscribe(listener);
        publisher.publish(StudentEventType.STUDENT_CREATED, UUID.randomUUID());
        publisher.unsubscribe(listener);
        publisher.publish(StudentEventType.STUDENT_DELETED, UUID.randomUUID());

        assertThat(received).extracting(StudentEvent::type).containsExactly(StudentEventType.STUDENT_CREATED);
    }

    @Test
    void failingListener_doesNotStopOtherListeners() {
        List<StudentEvent> received = new ArrayList<>();
        StudentEventListener failing = event -> { throw new IllegalStateException("boom"); };
        StudentEventPublisher publisher = new InMemoryStudentEventPublisher(List.of(failing, received::add), clock);

        publisher.publish(StudentEventType.STUDENT_CREATED, UUID.randomUUID());

        assertThat(received).hasSize(1);
    }

    @Test
    void activityLog_keepsMostRecentFirst_andCapsSize() {
        StudentActivityLog log = new StudentActivityLog();
        StudentEventPublisher publisher = new InMemoryStudentEventPublisher(List.of(log), clock);
        UUID studentId = UUID.randomUUID();

        for (int i = 0; i < StudentActivityLog.CAPACITY + 5; i++) {
            publisher.publish(StudentEventType.STUDENT_UPDATED, UUID.randomUUID());
        }
        publisher.publish(StudentEventType.STUDENT_CREATED, studentId);

        assertThat(log.recent()).hasSize(StudentActivityLog.CAPACITY);
        assertThat(log.recent().getFirst().studentId()).isEqualTo(studentId);
        assertThat(log.recentFor(studentId)).extracting(StudentEvent::type)
                .containsExactly(StudentEventType.STUDENT_CREATED);
    }
}
