package com.example.student_management.state;

import com.example.student_management.exceptions.InvalidStudentStateException;

/**
 * State: what a student may do, and which status they may move to, depends on
 * their current status. Each status has one stateless implementation of this
 * interface, so the rules live in the state classes instead of in
 * {@code if (status == ...)} checks spread across the service.
 *
 * <p>Permissions default to "not allowed" and transitions default to throwing
 * {@link InvalidStudentStateException}; each state overrides only what it allows.
 */
public interface StudentState {
    StudentStatus status();

    default boolean canEditProfile() {
        return false;
    }

    default boolean canBorrowBooks() {
        return false;
    }

    default boolean canEnroll() {
        return false;
    }

    default StudentState suspend() {
        throw notAllowed("suspend");
    }

    default StudentState reactivate() {
        throw notAllowed("reactivate");
    }

    default StudentState graduate() {
        throw notAllowed("graduate");
    }

    default StudentState withdraw() {
        throw notAllowed("withdraw");
    }

    private InvalidStudentStateException notAllowed(String transition) {
        return new InvalidStudentStateException("Cannot " + transition + " a student who is " + status());
    }
}
