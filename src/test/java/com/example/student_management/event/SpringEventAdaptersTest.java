package com.example.student_management.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Both adapters between our observer and Spring's events, in a real application context. */
class SpringEventAdaptersTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, () -> CLOCK)
            .withUserConfiguration(InMemoryStudentEventPublisher.class, SpringEventBridge.class, AnnotatedListener.class);

    /** A plain bean that knows nothing about StudentEventListener. */
    static class AnnotatedListener {
        final List<StudentEvent> received = new ArrayList<>();

        @EventListener
        void on(StudentEvent event) {
            received.add(event);
        }
    }

    /** A Spring listener that always fails. */
    static class FailingListener {
        @EventListener
        void on(StudentEvent event) {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void bridge_isSubscribedAutomatically_andReachesAtEventListenerMethods() {
        runner.run(context -> {
            UUID studentId = UUID.randomUUID();
            UUID courseId = UUID.randomUUID();

            context.getBean(StudentEventPublisher.class).publish(StudentEventType.COURSE_ENROLLED, studentId, courseId);

            assertThat(context.getBean(AnnotatedListener.class).received).containsExactly(
                    new StudentEvent(StudentEventType.COURSE_ENROLLED, studentId, courseId, CLOCK.instant()));
        });
    }

    @Test
    void aFailingSpringListener_doesNotBreakThePublisherOrTheOtherObservers() {
        List<StudentEvent> direct = new ArrayList<>();
        runner.withUserConfiguration(FailingListener.class)
                .withBean("direct", StudentEventListener.class, () -> direct::add)
                .run(context -> {
                    context.getBean(StudentEventPublisher.class).publish(StudentEventType.STUDENT_CREATED, UUID.randomUUID());

                    assertThat(direct).hasSize(1);
                });
    }

    @Test
    void adaptedListener_receivesStudentEventsThatReachSpring_once() {
        runner.run(context -> {
            List<StudentEvent> received = new ArrayList<>();
            context.addApplicationListener(StudentEventApplicationListener.adapt(received::add));

            context.getBean(StudentEventPublisher.class).publish(StudentEventType.STUDENT_DELETED, UUID.randomUUID());

            assertThat(received).hasSize(1);
            assertThat(received.getFirst().type()).isEqualTo(StudentEventType.STUDENT_DELETED);
        });
    }

    @Test
    void adaptedListener_ignoresOtherSpringEvents() {
        runner.run(context -> {
            List<StudentEvent> received = new ArrayList<>();
            context.addApplicationListener(StudentEventApplicationListener.adapt(received::add));

            context.publishEvent(new ContextRefreshedEvent(context));
            context.publishEvent("just a string");

            assertThat(received).isEmpty();
        });
    }
}
