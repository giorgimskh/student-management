package com.example.student_management.visitor;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;

/**
 * Visitor: one operation (a report, statistics, an audit) over the student object
 * graph. {@link Student#accept} visits the student, then each of their books,
 * courses and their photo, so a visitor sees a student followed by everything
 * that belongs to them. Implementations collect results as they go and expose
 * them through their own {@code result()} method. They are stateful, so use a
 * new instance for each run.
 */
public interface StudentGraphVisitor {
    void visit(Student student);

    void visit(Book book);

    void visit(Course course);

    void visit(StudentPhoto photo);
}
