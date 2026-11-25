package com.example.student_management.command;

import com.example.student_management.service.StudentService;

import java.util.UUID;

public record DropCourseCommand(StudentService studentService, UUID studentId, UUID courseId)
        implements StudentCommand<Void> {

    @Override
    public Void execute() {
        studentService.removeStudentFromCourse(studentId, courseId);
        return null;
    }

    @Override
    public void undo() {
        studentService.enrollInCourse(studentId, courseId);
    }

    @Override
    public String description() {
        return "Drop student " + studentId + " from course " + courseId;
    }

    @Override
    public <T> T accept(StudentCommandVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
