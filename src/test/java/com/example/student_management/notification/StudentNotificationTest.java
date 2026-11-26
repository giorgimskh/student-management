package com.example.student_management.notification;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The abstraction side: each kind of message, delivered through a channel that just records. */
class StudentNotificationTest {
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    record Delivery(Student recipient, String subject, String body, Instant sentAt) {
    }

    /** A channel that keeps what it was given. */
    static final class RecordingChannel implements NotificationChannel {
        final List<Delivery> deliveries = new ArrayList<>();

        @Override
        public String name() {
            return "recording";
        }

        @Override
        public void deliver(Student recipient, String subject, String body, Instant sentAt) {
            deliveries.add(new Delivery(recipient, subject, body, sentAt));
        }
    }

    private final RecordingChannel channel = new RecordingChannel();
    private final Student ana = Student.builder().id(UUID.randomUUID()).name("Ana").status(StudentStatus.ACTIVE).build();

    private StudentEvent event(StudentEventType type, UUID relatedId) {
        return new StudentEvent(type, ana.getId(), relatedId, NOW);
    }

    private Delivery only() {
        assertThat(channel.deliveries).hasSize(1);
        return channel.deliveries.getFirst();
    }

    @Test
    void welcome_isDeliveredThroughTheChannel_toTheStudent_atTheEventTime() {
        new WelcomeNotification(channel).send(ana, event(StudentEventType.STUDENT_CREATED, null));

        Delivery delivery = only();
        assertThat(delivery.recipient()).isSameAs(ana);
        assertThat(delivery.subject()).isEqualTo("Welcome, Ana");
        assertThat(delivery.sentAt()).isEqualTo(NOW);
    }

    @Test
    void overdueReminder_namesTheBookAndItsDueDate() {
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder().id(bookId).title("Clean Code").build();
        BookRepository books = mock(BookRepository.class);
        BookLoanRepository loans = mock(BookLoanRepository.class);
        when(books.findById(bookId)).thenReturn(Optional.of(book));
        when(loans.findFirstByBookIdAndReturnedAtIsNull(bookId))
                .thenReturn(Optional.of(BookLoan.builder().dueAt(Instant.parse("2026-02-20T09:00:00Z")).build()));

        new OverdueReminder(channel, books, loans).send(ana, event(StudentEventType.BOOK_OVERDUE, bookId));

        assertThat(only().subject()).isEqualTo("Overdue: Clean Code");
        assertThat(only().body()).contains("\"Clean Code\" was due back on 2026-02-20");
    }

    @Test
    void overdueReminder_stillSendsWhenTheBookIsGone() {
        new OverdueReminder(channel, mock(BookRepository.class), mock(BookLoanRepository.class))
                .send(ana, event(StudentEventType.BOOK_OVERDUE, UUID.randomUUID()));

        assertThat(only().subject()).isEqualTo("Overdue: a book");
        assertThat(only().body()).doesNotContain(" on ");
    }

    @Test
    void enrollmentNotice_coversEnrollingAndDropping() {
        UUID courseId = UUID.randomUUID();
        CourseRepository courses = mock(CourseRepository.class);
        when(courses.findById(courseId)).thenReturn(Optional.of(Course.builder().code("CS101").courseName("Algorithms").build()));
        EnrollmentNotice notice = new EnrollmentNotice(channel, courses);

        notice.send(ana, event(StudentEventType.COURSE_ENROLLED, courseId));
        notice.send(ana, event(StudentEventType.COURSE_DROPPED, courseId));

        assertThat(channel.deliveries).extracting(Delivery::subject)
                .containsExactly("Enrolled in CS101 Algorithms", "Dropped from CS101 Algorithms");
    }

    @Test
    void statusChangeNotice_hasAMessageForEveryStatusEvent() {
        StatusChangeNotice notice = new StatusChangeNotice(channel);

        notice.triggers().forEach(type -> notice.send(ana, event(type, null)));

        assertThat(channel.deliveries).hasSize(5);
        assertThat(channel.deliveries).extracting(Delivery::subject).doesNotHaveDuplicates();
        assertThat(channel.deliveries).extracting(Delivery::body).allSatisfy(body -> assertThat(body).isNotBlank());
    }

    @Test
    void statusRestored_saysTheStatusTheStudentNowHas() {
        Student suspended = Student.builder().id(ana.getId()).name("Ana").status(StudentStatus.SUSPENDED).build();

        new StatusChangeNotice(channel).send(suspended, event(StudentEventType.STUDENT_STATUS_RESTORED, null));

        assertThat(only().body()).endsWith("Your status is suspended.");
    }

    @Test
    void theSameMessage_goesThroughAnyChannel() {
        RecordingChannel other = new RecordingChannel();
        StudentEvent created = event(StudentEventType.STUDENT_CREATED, null);

        new WelcomeNotification(channel).send(ana, created);
        new WelcomeNotification(other).send(ana, created);

        assertThat(other.deliveries).isEqualTo(channel.deliveries);
    }

    @Test
    void eachEventType_isHandledByAtMostOneKindOfMessage() {
        List<StudentNotification> all = List.of(new WelcomeNotification(channel),
                new OverdueReminder(channel, mock(BookRepository.class), mock(BookLoanRepository.class)),
                new EnrollmentNotice(channel, mock(CourseRepository.class)), new StatusChangeNotice(channel));
        EnumSet<StudentEventType> seen = EnumSet.noneOf(StudentEventType.class);

        all.forEach(n -> n.triggers().forEach(type -> assertThat(seen.add(type)).as("%s handled twice", type).isTrue()));
    }
}
