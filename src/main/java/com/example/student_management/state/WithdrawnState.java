package com.example.student_management.state;

/** Final status: the record is read-only apart from giving back books and dropping courses. */
public final class WithdrawnState implements StudentState {
    public static final WithdrawnState INSTANCE = new WithdrawnState();

    private WithdrawnState() {
    }

    @Override
    public StudentStatus status() {
        return StudentStatus.WITHDRAWN;
    }
}
