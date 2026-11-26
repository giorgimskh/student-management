package com.example.student_management.command;

import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.service.StudentService;

import java.util.UUID;

public record RemoveBookCommand(StudentService studentService, UUID studentId, UUID bookId)
        implements StudentCommand<Void> {

    @Override
    public Void execute() {
        studentService.removeBookFromStudent(studentId, bookId);
        return null;
    }

    @Override
    public void undo() {
        studentService.assignBookToStudent(studentId, bookId);
    }

    @Override
    public String description() {
        return "Remove book " + bookId + " from student " + studentId;
    }

    /** Not repeatable: a book can be lent to only one student at a time. */
    @Override
    public boolean repeatable() {
        return false;
    }

    @Override
    public StudentCommand<Void> copyFor(UUID studentId) {
        throw new CommandNotRepeatableException("This action can't be repeated for another student: a book can be lent to only one student at a time");
    }

    @Override
    public <T> T accept(StudentCommandVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
