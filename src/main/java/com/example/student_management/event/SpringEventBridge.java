package com.example.student_management.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter from our observer to Spring's event system: it is a
 * {@link StudentEventListener} (the target, so {@link InMemoryStudentEventPublisher}
 * subscribes it like any other observer) that hands every event on to Spring's
 * {@link ApplicationEventPublisher} (the adaptee).
 *
 * <p>Any bean can then react to student events with Spring's own annotations,
 * without implementing our interface:
 *
 * <pre>{@code
 * @EventListener
 * void on(StudentEvent event) { ... }
 *
 * @TransactionalEventListener   // only once the change is committed
 * void afterCommit(StudentEvent event) { ... }
 * }</pre>
 *
 * Spring wraps the event in a {@code PayloadApplicationEvent}, so listeners
 * receive the {@link StudentEvent} itself. Delivery is on the publishing
 * thread, inside the caller's transaction, like every other observer. If a
 * Spring listener throws, the publisher logs it and the request carries on.
 */
@Component
public class SpringEventBridge implements StudentEventListener {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventBridge(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onStudentEvent(StudentEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
