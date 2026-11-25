package com.example.student_management.visitor;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;
import com.example.student_management.state.StudentStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Finds things that need follow-up. Status changes do not return books or drop
 * courses, so these can happen:
 * <ul>
 *   <li>a graduated or withdrawn student still has a book;</li>
 *   <li>a withdrawn student is still enrolled in a course;</li>
 *   <li>a photo's recorded size does not match its stored data.</li>
 * </ul>
 * Books, courses and the photo are checked against the student visited just
 * before them (see {@link Student#accept}).
 */
public class StudentAuditVisitor implements StudentGraphVisitor {
    private final List<AuditFinding> findings = new ArrayList<>();
    private Student current;

    @Override
    public void visit(Student student) {
        current = student;
    }

    @Override
    public void visit(Book book) {
        StudentStatus status = current.getStatus();
        if (status == StudentStatus.GRADUATED || status == StudentStatus.WITHDRAWN) {
            report(AuditFinding.Rule.BOOK_NOT_RETURNED, book.getId(),
                    "\"" + book.getTitle() + "\" is still on loan to a " + status + " student");
        }
    }

    @Override
    public void visit(Course course) {
        if (current.getStatus() == StudentStatus.WITHDRAWN) {
            report(AuditFinding.Rule.ENROLLED_AFTER_WITHDRAWAL, course.getId(),
                    "Still enrolled in " + course.label());
        }
    }

    @Override
    public void visit(StudentPhoto photo) {
        long actual = photo.getData() == null ? 0 : photo.getData().length;
        if (actual != photo.getSizeBytes()) {
            report(AuditFinding.Rule.PHOTO_SIZE_MISMATCH, photo.getId(),
                    "Photo is recorded as " + photo.getSizeBytes() + " bytes but has " + actual);
        }
    }

    public List<AuditFinding> result() {
        return List.copyOf(findings);
    }

    private void report(AuditFinding.Rule rule, UUID relatedId, String detail) {
        findings.add(new AuditFinding(rule, current.getId(), current.getName(), relatedId, detail));
    }
}
