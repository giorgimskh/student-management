package com.example.student_management.domain;

/**
 * Memento: an immutable snapshot of a {@link Student}'s editable state.
 * Only {@link Student} (same package) can read it; everyone else, such as a
 * command keeping it for undo, can only hold it and hand it back.
 */
public final class StudentMemento {
    private final String name;
    private final String email;

    StudentMemento(String name, String email) {
        this.name = name;
        this.email = email;
    }

    String name() {
        return name;
    }

    String email() {
        return email;
    }
}
