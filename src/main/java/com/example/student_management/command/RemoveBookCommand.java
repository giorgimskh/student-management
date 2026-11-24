package com.example.student_management.command;

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
}
