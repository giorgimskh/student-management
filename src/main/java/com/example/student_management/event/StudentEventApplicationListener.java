package com.example.student_management.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;

import java.util.Objects;

/**
 * Adapter the other way round: wraps one of our {@link StudentEventListener}s
 * (the adaptee) so it can be registered as a Spring {@link ApplicationListener}
 * (the target), e.g. with {@code context.addApplicationListener(...)}. It
 * passes on the {@link StudentEvent}s that reach Spring, such as those
 * forwarded by {@link SpringEventBridge}, and ignores every other Spring event.
 *
 * <p>Not a bean on its own: a {@link StudentEventListener} bean is already
 * subscribed to {@link StudentEventPublisher}, and registering it here as well
 * would deliver each event to it twice. Use it for listeners that live outside
 * that publisher, such as one added for a single test or a plug-in.
 */
public final class StudentEventApplicationListener implements ApplicationListener<ApplicationEvent> {
    private final StudentEventListener listener;

    private StudentEventApplicationListener(StudentEventListener listener) {
        this.listener = listener;
    }

    public static StudentEventApplicationListener adapt(StudentEventListener listener) {
        return new StudentEventApplicationListener(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payloadEvent
                && payloadEvent.getPayload() instanceof StudentEvent studentEvent) {
            listener.onStudentEvent(studentEvent);
        }
    }

    /** The wrapped listener. */
    public StudentEventListener listener() {
        return listener;
    }
}
