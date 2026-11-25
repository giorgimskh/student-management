package com.example.student_management.visitor;

/** An element of the student object graph that a {@link StudentGraphVisitor} can visit. */
public interface Visitable {
    void accept(StudentGraphVisitor visitor);
}
