package com.example.student_management.notification;

import com.example.student_management.TestcontainersConfiguration;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The inbox channel and the inbox reads, on Postgres with the V4 migration. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, InboxNotificationChannel.class, NotificationInbox.class, InboxRepositoryTest.FixedClock.class})
class InboxRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private InboxNotificationChannel channel;

    @Autowired
    private NotificationInbox inbox;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Student ana;
    private Student beka;

    @BeforeEach
    void setUp() {
        ana = entityManager.persist(Student.builder().name("Ana").email("ana@example.com").build());
        beka = entityManager.persist(Student.builder().name("Beka").email("beka@example.com").build());
        channel.deliver(ana, "First", "one", NOW.minusSeconds(60));
        channel.deliver(ana, "Second", "two ".repeat(200), NOW.minusSeconds(30)); // longer than a varchar(255)
        channel.deliver(beka, "Other", "not Ana's", NOW);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void recent_isOnlyTheStudentsOwn_newestFirst_andUnread() {
        assertThat(inbox.recent(ana.getId())).extracting(Notification::getSubject).containsExactly("Second", "First");
        assertThat(inbox.recent(ana.getId())).allSatisfy(n -> assertThat(n.getReadAt()).isNull());
        assertThat(inbox.unreadCount(ana.getId())).isEqualTo(2);
    }

    @Test
    void markAllRead_marksOnlyThatStudent_once() {
        assertThat(inbox.markAllRead(ana.getId())).isEqualTo(2);
        assertThat(inbox.markAllRead(ana.getId())).isZero();
        entityManager.clear();

        assertThat(inbox.unreadCount(ana.getId())).isZero();
        assertThat(inbox.recent(ana.getId())).allSatisfy(n -> assertThat(n.getReadAt()).isEqualTo(NOW));
        assertThat(inbox.unreadCount(beka.getId())).isEqualTo(1);
    }

    @Test
    void unknownStudent_is404() {
        assertThatThrownBy(() -> inbox.recent(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> inbox.markAllRead(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletingTheStudent_deletesTheirInbox() {
        entityManager.remove(entityManager.find(Student.class, ana.getId()));
        entityManager.flush();

        assertThat(notificationRepository.count()).isEqualTo(1);
    }
}
