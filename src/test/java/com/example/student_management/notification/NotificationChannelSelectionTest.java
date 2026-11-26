package com.example.student_management.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Exactly one channel exists, and every kind of message is bridged to it. */
class NotificationChannelSelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(NotificationRepository.class, () -> mock(NotificationRepository.class))
            .withUserConfiguration(InboxNotificationChannel.class, LogNotificationChannel.class,
                    WelcomeNotification.class, StatusChangeNotice.class);

    @Test
    void defaultsToTheInbox() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(NotificationChannel.class);
            assertThat(context.getBean(NotificationChannel.class)).isInstanceOf(InboxNotificationChannel.class);
            assertThat(context.getBeansOfType(StudentNotification.class).values())
                    .allSatisfy(n -> assertThat(n.channel()).isInstanceOf(InboxNotificationChannel.class));
        });
    }

    @Test
    void logWhenConfigured_withoutChangingTheMessages() {
        runner.withPropertyValues("app.notifications.channel=log").run(context -> {
            assertThat(context).hasSingleBean(NotificationChannel.class);
            assertThat(context.getBeansOfType(StudentNotification.class).values()).hasSize(2)
                    .allSatisfy(n -> assertThat(n.channel()).isInstanceOf(LogNotificationChannel.class));
        });
    }
}
