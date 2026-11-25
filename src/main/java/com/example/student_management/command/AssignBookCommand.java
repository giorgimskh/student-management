package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;

import java.util.UUID;

public record AssignBookCommand(StudentService studentService, UUID studentId, UUID bookId)
        implements StudentCommand<Student> {

    @Override
    public Student execute() {
        return studentService.assignBookToStudent(studentId, bookId);
    }

    @Override
    public void undo() {
        studentService.removeBookFromStudent(studentId, bookId);
    }

    @Override
    public String description() {
        return "Assign book " + bookId + " to student " + studentId;
    }

    @Override
    public <T> T accept(StudentCommandVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
