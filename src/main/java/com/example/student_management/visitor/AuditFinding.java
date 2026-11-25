package com.example.student_management.visitor;

import java.util.UUID;

/** One problem found by {@link StudentAuditVisitor}. {@code relatedId} is the book, course or photo involved. */
public record AuditFinding(Rule rule, UUID studentId, String studentName, UUID relatedId, String detail) {
    public enum Rule {
        BOOK_NOT_RETURNED,
        ENROLLED_AFTER_WITHDRAWAL,
        PHOTO_SIZE_MISMATCH
    }
}
