package com.example.student_management.command;

import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentMemento;
import com.example.student_management.service.StudentService;

import java.util.UUID;

/**
 * Updates a student's profile. Before changing anything it takes a
 * {@link StudentMemento} of the current state (acting as the memento's
 * caretaker) and hands it back to the service on undo.
 */
public final class UpdateStudentCommand implements StudentCommand<Student> {
    private final StudentService studentService;
    private final UUID studentId;
    private final Student details;
    private StudentMemento before;

    public UpdateStudentCommand(StudentService studentService, UUID studentId, Student details) {
        this.studentService = studentService;
        this.studentId = studentId;
        this.details = details;
    }

    @Override
    public Student execute() {
        before = studentService.getStudentById(studentId).createMemento();
        return studentService.updateStudent(studentId, details);
    }

    @Override
    public void undo() {
        studentService.restoreStudent(studentId, before);
    }

    @Override
    public String description() {
        return "Update profile of student " + studentId;
    }

    @Override
    public UUID studentId() {
        return studentId;
    }

    /** The new profile values this command applies. */
    public Student details() {
        return details;
    }

    /** Not repeatable: another student cannot be given the same name and email. */
    @Override
    public boolean repeatable() {
        return false;
    }

    @Override
    public StudentCommand<Student> copyFor(UUID studentId) {
        throw new CommandNotRepeatableException("This action can't be repeated for another student: another student cannot be given the same name and email");
    }

    @Override
    public <T> T accept(StudentCommandVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
