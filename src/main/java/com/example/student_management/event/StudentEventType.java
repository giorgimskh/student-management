package com.example.student_management.event;

public enum StudentEventType {
    STUDENT_CREATED,
    STUDENT_UPDATED,
    STUDENT_DELETED,
    BOOK_ASSIGNED,
    BOOK_REMOVED,
    COURSE_ENROLLED,
    COURSE_DROPPED,
    STUDENT_SUSPENDED,
    STUDENT_REACTIVATED,
    STUDENT_GRADUATED,
    STUDENT_WITHDRAWN,
    /** An undo put the student back to an earlier status. */
    STUDENT_STATUS_RESTORED,
    /** A borrowed book passed its due date ({@code relatedId} is the book). */
    BOOK_OVERDUE
}
