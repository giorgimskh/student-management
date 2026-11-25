package com.example.student_management.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

/**
 * One time a student borrowed a book. {@link Book#getStudent()} is still the
 * current borrower; a loan adds the due date and keeps the history after the
 * book is given back.
 */
@Entity
@Table(name = "book_loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookLoan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    private Student student;

    @Column(nullable = false)
    private Instant borrowedAt;

    @Column(nullable = false)
    private Instant dueAt;

    /** Null while the book is still borrowed. */
    private Instant returnedAt;

    /** When the overdue check reported this loan, so it is reported only once. */
    private Instant overdueNotifiedAt;

    public boolean isOpen() {
        return returnedAt == null;
    }

    public boolean isOverdue(Instant now) {
        return isOpen() && dueAt.isBefore(now);
    }
}
