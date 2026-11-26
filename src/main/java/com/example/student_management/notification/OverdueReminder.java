package com.example.student_management.notification;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.BookLoan;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEvent;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.repository.BookLoanRepository;
import com.example.student_management.repository.BookRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/** Sent when a borrowed book passes its due date ({@code relatedId} is the book). */
@Component
public class OverdueReminder extends StudentNotification {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final BookRepository bookRepository;
    private final BookLoanRepository loanRepository;

    public OverdueReminder(NotificationChannel channel, BookRepository bookRepository, BookLoanRepository loanRepository) {
        super(channel);
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public Set<StudentEventType> triggers() {
        return Set.of(StudentEventType.BOOK_OVERDUE);
    }

    @Override
    protected String subject(Student student, StudentEvent event) {
        return "Overdue: " + title(event);
    }

    @Override
    protected String body(Student student, StudentEvent event) {
        String due = event.relatedId() == null ? null : loanRepository.findFirstByBookIdAndReturnedAtIsNull(event.relatedId())
                .map(BookLoan::getDueAt).map(DAY::format).orElse(null);
        return "\"" + title(event) + "\" was due back" + (due == null ? "" : " on " + due)
                + ". Please return it to the library as soon as you can.";
    }

    private String title(StudentEvent event) {
        return event.relatedId() == null ? "a book"
                : bookRepository.findById(event.relatedId()).map(Book::getTitle).orElse("a book");
    }
}
