package com.example.student_management.state;

/** Final status: the record is read-only apart from giving back books and dropping courses. */
public final class GraduatedState implements StudentState {
    public static final GraduatedState INSTANCE = new GraduatedState();

    private GraduatedState() {
    }

    @Override
    public StudentStatus status() {
        return StudentStatus.GRADUATED;
    }
}
